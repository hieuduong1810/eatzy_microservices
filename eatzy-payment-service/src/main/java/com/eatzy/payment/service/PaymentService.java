package com.eatzy.payment.service;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.designpattern.adapter.OrderServiceClient;
import com.eatzy.payment.designpattern.strategy.PaymentStrategy;
import com.eatzy.payment.designpattern.strategy.PaymentStrategyFactory;
import com.eatzy.payment.domain.enums.TransactionType;
import com.eatzy.payment.domain.Voucher;
import com.eatzy.payment.domain.Wallet;
import com.eatzy.payment.domain.WalletTransaction;
import com.eatzy.payment.designpattern.adapter.SystemConfigServiceClient;
import com.eatzy.payment.dto.request.ReqPaymentInitiateDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private final WalletService walletService;
    private final WalletTransactionService walletTransactionService;
    private final VoucherService voucherService;
    private final AuthServiceClient authServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final SystemConfigServiceClient systemConfigServiceClient;

    public PaymentService(WalletService walletService,
            WalletTransactionService walletTransactionService,
            VoucherService voucherService,
            AuthServiceClient authServiceClient,
            OrderServiceClient orderServiceClient,
            PaymentStrategyFactory paymentStrategyFactory,
            SystemConfigServiceClient systemConfigServiceClient) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.voucherService = voucherService;
        this.authServiceClient = authServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.paymentStrategyFactory = paymentStrategyFactory;
        this.systemConfigServiceClient = systemConfigServiceClient;
    }

    private Long getAdminId() {
        try {
            Map<String, Object> response = authServiceClient.getUserByEmail("admin@gmail.com");
            if (response != null && response.containsKey("id")) {
                return ((Number) response.get("id")).longValue();
            }
        } catch (Exception e) {
            System.err.println("Error fetching getAdminId from auth service: " + e.getMessage());
        }
        return null;
    }

    public Map<String, Object> initiatePayment(ReqPaymentInitiateDTO req)
            throws Exception {
        PaymentStrategy strategy = paymentStrategyFactory
                .getStrategy(req.getMethod());
        return strategy.initiatePayment(req);
    }

    public Map<String, Object> handleCallback(String method, Map<String, String> queryParams) throws Exception {
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(method);
        return strategy.processCallback(queryParams);
    }

    @Transactional
    public Map<String, Object> processRefund(Long orderId, Long customerId, BigDecimal refundAmount)
            throws IdInvalidException {
        Map<String, Object> result = new HashMap<>();

        Long adminId = getAdminId();
        if (adminId == null)
            throw new IdInvalidException("Admin user not found");

        Wallet adminWallet = walletService.getWalletByUserId(adminId);
        if (adminWallet == null)
            throw new IdInvalidException("Admin wallet not found");

        if (adminWallet.getBalance().compareTo(refundAmount) < 0) {
            result.put("success", false);
            result.put("message", "Insufficient admin wallet balance for refund");
            return result;
        }

        Wallet customerWallet = walletService.getWalletByUserId(customerId);
        if (customerWallet == null)
            throw new IdInvalidException("Customer wallet not found");

        WalletTransaction adminTransaction = WalletTransaction.builder()
                .wallet(adminWallet)
                .transactionType(TransactionType.REFUND)
                .amount(refundAmount.negate())
                .balanceAfter(adminWallet.getBalance().subtract(refundAmount))
                .description("Refund for order #" + orderId)
                .orderId(orderId)
                .status("SUCCESS")
                .transactionDate(Instant.now())
                .build();
        walletTransactionService.createWalletTransaction(adminTransaction);

        WalletTransaction customerTransaction = WalletTransaction.builder()
                .wallet(customerWallet)
                .transactionType(TransactionType.REFUND)
                .amount(refundAmount)
                .balanceAfter(customerWallet.getBalance().add(refundAmount))
                .description("Refund for order #" + orderId)
                .orderId(orderId)
                .status("SUCCESS")
                .transactionDate(Instant.now())
                .build();
        walletTransactionService.createWalletTransaction(customerTransaction);

        result.put("success", true);
        result.put("message", "Refund successful");
        return result;
    }

    @Transactional
    public Map<String, Object> processCODPaymentOnDelivery(Long orderId, Long driverId, BigDecimal totalAmount)
            throws IdInvalidException {
        Map<String, Object> result = new HashMap<>();

        Wallet driverWallet = walletService.getWalletByUserId(driverId);
        if (driverWallet == null)
            throw new IdInvalidException("Driver wallet not found");

        WalletTransaction driverTransaction = WalletTransaction.builder()
                .wallet(driverWallet)
                .transactionType(TransactionType.PAYMENT)
                .amount(totalAmount.negate())
                .balanceAfter(driverWallet.getBalance().subtract(totalAmount))
                .description("Payment for order #" + orderId)
                .orderId(orderId)
                .status("SUCCESS")
                .transactionDate(Instant.now())
                .build();
        walletTransactionService.createWalletTransaction(driverTransaction);

        Long adminId = getAdminId();
        if (adminId != null) {
            Wallet adminWallet = walletService.getWalletByUserId(adminId);
            if (adminWallet != null) {
                WalletTransaction adminTransaction = WalletTransaction.builder()
                        .wallet(adminWallet)
                        .transactionType(TransactionType.COD_RECEIVED)
                        .amount(totalAmount)
                        .balanceAfter(adminWallet.getBalance().add(totalAmount))
                        .description("COD payment received from order #" + orderId)
                        .orderId(orderId)
                        .status("SUCCESS")
                        .transactionDate(Instant.now())
                        .build();
                walletTransactionService.createWalletTransaction(adminTransaction);
            }
        }

        result.put("success", true);
        result.put("message", "COD payment recorded successfully");
        return result;
    }

    public Map<String, BigDecimal> calculateCommissions(BigDecimal deliveryFee, BigDecimal subtotal) {
        Map<String, BigDecimal> commissions = new HashMap<>();

        BigDecimal driverCommissionPercent = getSystemConfigValue("DRIVER_COMMISSION_PERCENT", new BigDecimal("15"));
        BigDecimal restaurantCommissionPercent = getSystemConfigValue("RESTAURANT_COMMISSION_PERCENT",
                new BigDecimal("20"));

        BigDecimal driverCommission = BigDecimal.ZERO;
        if (deliveryFee != null) {
            driverCommission = deliveryFee.multiply(driverCommissionPercent).divide(new BigDecimal("100"), 2,
                    BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal restaurantCommission = BigDecimal.ZERO;
        if (subtotal != null) {
            restaurantCommission = subtotal.multiply(restaurantCommissionPercent).divide(new BigDecimal("100"), 2,
                    BigDecimal.ROUND_HALF_UP);
        }

        commissions.put("driverCommission", driverCommission);
        commissions.put("restaurantCommission", restaurantCommission);
        commissions.put("driverEarnings",
                deliveryFee != null ? deliveryFee.subtract(driverCommission) : BigDecimal.ZERO);
        commissions.put("restaurantEarnings",
                subtotal != null ? subtotal.subtract(restaurantCommission) : BigDecimal.ZERO);
        return commissions;
    }

    public BigDecimal calculateVoucherDiscount(List<Long> voucherIds, BigDecimal subtotal, Long restaurantId,
            Long customerId, BigDecimal deliveryFee) {
        if (voucherIds == null || voucherIds.isEmpty() || subtotal == null)
            return BigDecimal.ZERO;

        BigDecimal totalDiscount = BigDecimal.ZERO;
        Instant now = Instant.now();

        for (Long vId : voucherIds) {
            Voucher v = voucherService.findVoucherEntityById(vId);
            if (v == null || !v.getActive())
                continue;

            // Time validation
            if (v.getStartDate() != null && now.isBefore(v.getStartDate()))
                continue;
            if (v.getEndDate() != null && now.isAfter(v.getEndDate()))
                continue;

            // Quantity validation
            if (v.getRemainingQuantity() != null && v.getRemainingQuantity() <= 0)
                continue;

            // Minimum order value validation
            if (v.getMinOrderValue() != null && subtotal.compareTo(v.getMinOrderValue()) < 0)
                continue;

            // Restaurant validation
            if (v.getRestaurantIds() != null && !v.getRestaurantIds().isEmpty()) {
                if (!v.getRestaurantIds().contains(restaurantId))
                    continue;
            }

            // Usage validation
            if (customerId != null && v.getUsageLimitPerUser() != null) {
                try {
                    Long usageCount = orderServiceClient.countByCustomerIdAndVoucherId(customerId, vId);
                    if (usageCount != null && usageCount >= v.getUsageLimitPerUser()) {
                        continue; // limit reached
                    }
                } catch (Exception e) {
                    continue; // fail-safe: ignore voucher if checking fails
                }
            }

            BigDecimal currentDiscount = BigDecimal.ZERO;
            if ("PERCENTAGE".equalsIgnoreCase(v.getDiscountType())) {
                currentDiscount = subtotal.multiply(v.getDiscountValue()).divide(new BigDecimal("100"), 2,
                        BigDecimal.ROUND_HALF_UP);
                if (v.getMaxDiscountAmount() != null && currentDiscount.compareTo(v.getMaxDiscountAmount()) > 0) {
                    currentDiscount = v.getMaxDiscountAmount();
                }
            } else if ("FIXED".equalsIgnoreCase(v.getDiscountType())) {
                currentDiscount = v.getDiscountValue();
            } else if ("FREESHIP".equalsIgnoreCase(v.getDiscountType()) && deliveryFee != null) {
                // Free shipping: discount is limited by voucher's discountValue
                // If deliveryFee < discountValue, only deduct deliveryFee (don't over-discount)
                // If deliveryFee >= discountValue, only deduct discountValue (cap at voucher
                // value)
                if (deliveryFee.compareTo(v.getDiscountValue()) < 0) {
                    currentDiscount = deliveryFee;
                } else {
                    currentDiscount = v.getDiscountValue();
                }
            }

            totalDiscount = totalDiscount.add(currentDiscount);
            // Optionally, we could limit discount so it doesn't exceed subtotal
            if (totalDiscount.compareTo(subtotal) >= 0) {
                return subtotal;
            }
        }

        return totalDiscount;
    }

    private BigDecimal getSystemConfigValue(String key, BigDecimal defaultValue) {
        try {
            Map<String, Object> configData = systemConfigServiceClient.getSystemConfigurationByKey(key);
            if (configData != null && configData.get("configValue") != null) {
                return new BigDecimal(configData.get("configValue").toString());
            }
        } catch (Exception e) {
            // fallback to default
        }
        return defaultValue;
    }
}
