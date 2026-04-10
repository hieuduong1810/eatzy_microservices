package com.eatzy.payment.kafka;

import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.payment.service.VoucherService;
import com.eatzy.payment.service.WalletTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
@Component
public class PaymentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final WalletTransactionService walletTransactionService;
    private final VoucherService voucherService;
    private final AuthServiceClient authServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;

    public PaymentEventConsumer(WalletTransactionService walletTransactionService,
                                VoucherService voucherService,
                                AuthServiceClient authServiceClient,
                                RestaurantServiceClient restaurantServiceClient) {
        this.walletTransactionService = walletTransactionService;
        this.voucherService = voucherService;
        this.authServiceClient = authServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
    }

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    public void consumeOrderEvents(String recordJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> record = objectMapper.readValue(recordJson, new TypeReference<Map<String, Object>>() {});
            // Because order-events may contain different types, we must identify OrderDeliveredEvent.
            // Check if it has driverNetEarning or restaurantNetEarning to ensure it's OrderDeliveredEvent
            if (record.containsKey("driverNetEarning") && record.containsKey("restaurantNetEarning")) {
                log.info("📥 Processing OrderDeliveredEvent for earnings distribution");

                Long orderId = getLongValue(record, "orderId");
                Long driverId = getLongValue(record, "driverId");
                Long restaurantId = getLongValue(record, "restaurantId");
                BigDecimal driverNetEarning = getBigDecimalValue(record, "driverNetEarning");
                BigDecimal restaurantNetEarning = getBigDecimalValue(record, "restaurantNetEarning");
                List<Long> voucherIds = getListValue(record, "voucherIds");

                // 1. Decrement Voucher Quantity
                if (voucherIds != null && !voucherIds.isEmpty()) {
                    voucherService.decrementVoucherQuantity(voucherIds);
                }

                // 2. Distribute driver earnings
                if (driverId != null && driverNetEarning.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        Map<String, Object> driverRes = authServiceClient.getDriverProfileByUserId(driverId);
                        // If driver exists and wallet is connected
                        walletTransactionService.depositToWalletByUserId(driverId, driverNetEarning, 
                                "Delivery earning from order #" + orderId, orderId, com.eatzy.payment.domain.enums.TransactionType.DELIVERY_EARNING);
                        log.info("💰 Added {} to driver {} wallet for order {}", driverNetEarning, driverId, orderId);
                    } catch (Exception e) {
                        log.error("Failed to deposit driver earnings: {}", e.getMessage());
                    }
                }

                // 3. Distribute restaurant owner earnings
                if (restaurantId != null && restaurantNetEarning.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        Map<String, Object> restaurantResponse = restaurantServiceClient.getRestaurantById(restaurantId);
                        if (restaurantResponse != null) {
                            Map<String, Object> restaurantData = restaurantResponse;
                            Long ownerId = null;
                            if (restaurantData.containsKey("user")) { // ResRestaurantDTO has nested user
                                Map<String, Object> owner = (Map<String, Object>) restaurantData.get("user");
                                ownerId = getLongValue(owner, "id");
                            } else if (restaurantData.containsKey("ownerId")) {
                                ownerId = getLongValue(restaurantData, "ownerId");
                            }

                            if (ownerId != null) {
                                String name = restaurantData.containsKey("name") ? (String) restaurantData.get("name") : "Unknown";
                                walletTransactionService.depositToWalletByUserId(ownerId, restaurantNetEarning, 
                                        "Restaurant earning from order #" + orderId + " (" + name + ")", orderId, com.eatzy.payment.domain.enums.TransactionType.RESTAURANT_EARNING);
                                log.info("💰 Added {} to restaurant owner {} wallet for order {}", restaurantNetEarning, ownerId, orderId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to deposit restaurant earnings: {}", e.getMessage());
                    }
                }

                // 4. Deduct commission from admin wallet
                BigDecimal totalCommission = driverNetEarning.add(restaurantNetEarning);
                if (totalCommission.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        Map<String, Object> adminResponse = authServiceClient.getUserByRoleName("ADMIN");
                        if (adminResponse != null) {
                            Map<String, Object> adminData = adminResponse;
                            Long adminId = getLongValue(adminData, "id");

                            if (adminId != null) {
                                walletTransactionService.withdrawFromWalletByUserId(adminId, totalCommission, 
                                        "Commission paid to driver and restaurant for order #" + orderId, orderId, com.eatzy.payment.domain.enums.TransactionType.COMMISSION_PAID);
                                log.info("📉 Deducted {} commission from admin wallet for order {}", totalCommission, orderId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to deduct commission from admin: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return new BigDecimal(value.toString());
    }

    private List<Long> getListValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(item -> {
                        if (item instanceof Number) return ((Number) item).longValue();
                        return Long.parseLong(item.toString());
                    })
                    .collect(Collectors.toList());
        }
        return null;
    }
}
