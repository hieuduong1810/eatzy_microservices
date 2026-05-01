package com.eatzy.order.mapper;

import com.eatzy.common.service.MapboxService;
import com.eatzy.order.designpattern.adapter.AuthServiceClient;
import com.eatzy.order.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.order.designpattern.adapter.PaymentServiceClient;
import com.eatzy.order.domain.Order;
import com.eatzy.order.domain.OrderEarningsSummary;
import com.eatzy.order.domain.OrderItem;
import com.eatzy.order.domain.OrderItemOption;
import com.eatzy.order.dto.response.ResOrderDTO;
import com.eatzy.order.dto.response.ResOrderItemDTO;
import com.eatzy.order.dto.response.ResOrderItemOptionDTO;
import com.eatzy.order.repository.OrderEarningsSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper: Converts Order entities to DTOs, enriching with data from external
 * services via Adapter pattern.
 */
@Component
public class OrderMapper {

    private static final Logger log = LoggerFactory.getLogger(OrderMapper.class);

    private final AuthServiceClient authServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderEarningsSummaryRepository orderEarningsSummaryRepository;
    private final MapboxService mapboxService;

    public OrderMapper(AuthServiceClient authServiceClient,
            RestaurantServiceClient restaurantServiceClient,
            PaymentServiceClient paymentServiceClient,
            OrderEarningsSummaryRepository orderEarningsSummaryRepository,
            MapboxService mapboxService) {
        this.authServiceClient = authServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.orderEarningsSummaryRepository = orderEarningsSummaryRepository;
        this.mapboxService = mapboxService;
    }

    public ResOrderDTO toResOrderDTO(Order order) {
        ResOrderDTO dto = new ResOrderDTO();
        dto.setId(order.getId());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setDeliveryLatitude(order.getDeliveryLatitude());
        dto.setDeliveryLongitude(order.getDeliveryLongitude());
        dto.setSpecialInstructions(order.getSpecialInstructions());
        dto.setSubtotal(order.getSubtotal());
        dto.setDeliveryFee(formatDecimal(order.getDeliveryFee()));
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setTotalAmount(formatDecimal(order.getTotalAmount()));
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setCancellationReason(order.getCancellationReason());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setPreparingAt(order.getPreparingAt());
        dto.setDeliveredAt(order.getDeliveredAt());

        // Enrich with Vouchers
        if (order.getVoucherIds() != null && !order.getVoucherIds().isEmpty()) {
            List<ResOrderDTO.Voucher> voucherDTOs = order.getVoucherIds().stream().map(voucherId -> {
                try {
                    Map<String, Object> voucherData = paymentServiceClient.getVoucherById(voucherId);
                    if (voucherData != null) {
                        return new ResOrderDTO.Voucher(voucherId, getStringValue(voucherData, "code"));
                    }
                } catch (Exception e) {
                    log.warn("Failed to enrich voucher info for voucherId {}: {}", voucherId, e.getMessage());
                }
                return null; // Return null if fetching fails
            }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
            dto.setVouchers(voucherDTOs);
        }

        // Calculate total trip duration
        if (order.getCreatedAt() != null && order.getDeliveredAt() != null) {
            Duration duration = Duration.between(order.getCreatedAt(), order.getDeliveredAt());
            dto.setTotalTripDuration(duration.toMinutes());
        }

        // Enrich customer info from Auth Service via Adapter
        if (order.getCustomerId() != null) {
            try {
                Map<String, Object> userData = authServiceClient.getUserById(order.getCustomerId());
                if (userData != null) {
                    ResOrderDTO.Customer customer = new ResOrderDTO.Customer();
                    customer.setId(order.getCustomerId());
                    customer.setName(getStringValue(userData, "name"));
                    customer.setEmail(getStringValue(userData, "email"));
                    customer.setPhoneNumber(getStringValue(userData, "phoneNumber"));
                    dto.setCustomer(customer);
                }
            } catch (Exception e) {
                log.warn("Failed to enrich customer info for order {}: {}", order.getId(), e.getMessage());
                dto.setCustomer(new ResOrderDTO.Customer(order.getCustomerId(), null, null, null));
            }
        }

        // Enrich restaurant info from Restaurant Service via Adapter
        if (order.getRestaurantId() != null) {
            try {
                Map<String, Object> restData = restaurantServiceClient.getRestaurantById(order.getRestaurantId());
                if (restData != null) {
                    ResOrderDTO.Restaurant restaurant = new ResOrderDTO.Restaurant();
                    restaurant.setId(order.getRestaurantId());
                    restaurant.setName(getStringValue(restData, "name"));
                    restaurant.setSlug(getStringValue(restData, "slug"));
                    restaurant.setAddress(getStringValue(restData, "address"));
                    restaurant.setImageUrl(getStringValue(restData, "avatarUrl"));
                    restaurant.setLatitude(getBigDecimalValue(restData, "latitude"));
                    restaurant.setLongitude(getBigDecimalValue(restData, "longitude"));
                    dto.setRestaurant(restaurant);

                    // Calculate distance using Mapbox directly
                    if (restaurant.getLatitude() != null && restaurant.getLongitude() != null
                            && order.getDeliveryLatitude() != null && order.getDeliveryLongitude() != null) {
                        BigDecimal distance = mapboxService.getDrivingDistance(
                                restaurant.getLatitude(), restaurant.getLongitude(),
                                order.getDeliveryLatitude(), order.getDeliveryLongitude());
                        dto.setDistance(formatDecimal(distance));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to enrich restaurant info for order {}: {}", order.getId(), e.getMessage());
            }
        }

        // Enrich driver info from Auth Service via Adapter
        if (order.getDriverId() != null) {
            try {
                Map<String, Object> driverData = authServiceClient.getUserById(order.getDriverId());
                Map<String, Object> driverProfile = authServiceClient.getDriverProfileByUserId(order.getDriverId());
                log.info("[OrderMapper] Enrich driver for orderId={}, driverId={}, driverData={}, driverProfile={}",
                        order.getId(), order.getDriverId(), driverData, driverProfile);
                if (driverData != null) {
                    ResOrderDTO.Driver driver = new ResOrderDTO.Driver();
                    driver.setId(order.getDriverId());
                    driver.setName(getStringValue(driverData, "name"));
                    driver.setEmail(getStringValue(driverData, "email"));
                    driver.setPhoneNumber(getStringValue(driverData, "phoneNumber"));
                    if (driverProfile != null) {
                        Map<String, Object> driverProfileUser = getMapValue(driverProfile, "user");
                        driver.setVehicleType(getStringValue(driverProfile, "vehicle_type"));
                        if (driver.getVehicleType() == null) {
                            driver.setVehicleType(getStringValue(driverProfile, "vehicleType"));
                        }
                        driver.setAverageRating(getStringValue(driverProfile, "averageRating"));
                        driver.setCompletedTrips(getStringValue(driverProfile, "completedTrips"));
                        driver.setVehicleLicensePlate(getStringValue(driverProfile, "vehicle_license_plate"));
                        if (driver.getVehicleLicensePlate() == null) {
                            driver.setVehicleLicensePlate(getStringValue(driverProfile, "vehicleLicensePlate"));
                        }
                        driver.setVehicleDetails(getStringValue(driverProfile, "vehicleDetails"));
                        if (driver.getPhoneNumber() == null && driverProfileUser != null) {
                            driver.setPhoneNumber(getStringValue(driverProfileUser, "phoneNumber"));
                            if (driver.getPhoneNumber() == null) {
                                driver.setPhoneNumber(getStringValue(driverProfileUser, "phone_number"));
                            }
                        }
                    }
                    log.info("[OrderMapper] Mapped driver for orderId={}, driverId={}, name={}, email={}, phoneNumber={}, vehicleType={}, vehicleDetails={}, averageRating={}, completedTrips={}, vehicleLicensePlate={}",
                            order.getId(),
                            order.getDriverId(),
                            driver.getName(),
                            driver.getEmail(),
                            driver.getPhoneNumber(),
                            driver.getVehicleType(),
                            driver.getVehicleDetails(),
                            driver.getAverageRating(),
                            driver.getCompletedTrips(),
                            driver.getVehicleLicensePlate());
                    dto.setDriver(driver);
                }
            } catch (Exception e) {
                log.warn("Failed to enrich driver info for order {}: {}", order.getId(), e.getMessage());
            }
        }

        // Convert order items
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<ResOrderItemDTO> orderItemDtos = order.getOrderItems().stream()
                    .map(this::toResOrderItemDTO)
                    .collect(Collectors.toList());
            dto.setOrderItems(orderItemDtos);
        }

        // Get earnings summary
        Optional<OrderEarningsSummary> earningsOpt = orderEarningsSummaryRepository.findByOrderId(order.getId());
        if (earningsOpt.isPresent()) {
            OrderEarningsSummary earnings = earningsOpt.get();
            dto.setRestaurantCommissionAmount(earnings.getRestaurantCommissionAmount());
            dto.setRestaurantNetEarning(earnings.getRestaurantNetEarning());
            dto.setDriverCommissionAmount(earnings.getDriverCommissionAmount());
            dto.setDriverNetEarning(earnings.getDriverNetEarning());
        }

        return dto;
    }

    public ResOrderItemDTO toResOrderItemDTO(OrderItem orderItem) {
        ResOrderItemDTO dto = new ResOrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPriceAtPurchase(orderItem.getPriceAtPurchase());

        // Enrich dish info from Restaurant Service
        if (orderItem.getDishId() != null) {
            try {
                Map<String, Object> dishData = restaurantServiceClient.getDishById(orderItem.getDishId());
                if (dishData != null) {
                    ResOrderItemDTO.Dish dish = new ResOrderItemDTO.Dish();
                    dish.setId(orderItem.getDishId());
                    dish.setName(getStringValue(dishData, "name"));
                    dish.setPrice(getBigDecimalValue(dishData, "price"));
                    dto.setDish(dish);
                }
            } catch (Exception e) {
                log.warn("Failed to enrich dish info for order item {}: {}", orderItem.getId(), e.getMessage());
            }
        }

        // Convert options
        if (orderItem.getOrderItemOptions() != null && !orderItem.getOrderItemOptions().isEmpty()) {
            List<ResOrderItemOptionDTO> optionDtos = orderItem.getOrderItemOptions().stream()
                    .map(this::toResOrderItemOptionDTO)
                    .collect(Collectors.toList());
            dto.setOrderItemOptions(optionDtos);
        }

        return dto;
    }

    public ResOrderItemOptionDTO toResOrderItemOptionDTO(OrderItemOption option) {
        ResOrderItemOptionDTO dto = new ResOrderItemOptionDTO();
        dto.setId(option.getId());
        dto.setOptionName(option.getOptionName());
        dto.setPriceAtPurchase(option.getPriceAtPurchase());

        // Enrich menu option info from Restaurant Service
        if (option.getMenuOptionId() != null) {
            try {
                Map<String, Object> menuOptionData = restaurantServiceClient
                        .getMenuOptionById(option.getMenuOptionId());
                if (menuOptionData != null) {
                    ResOrderItemOptionDTO.MenuOption menuOption = new ResOrderItemOptionDTO.MenuOption();
                    menuOption.setId(option.getMenuOptionId());
                    menuOption.setName(getStringValue(menuOptionData, "name"));
                    menuOption.setPriceAdjustment(getBigDecimalValue(menuOptionData, "priceAdjustment"));
                    dto.setMenuOption(menuOption);
                }
            } catch (Exception e) {
                log.warn("Failed to enrich menu option info: {}", e.getMessage());
            }
        }

        return dto;
    }

    // Helper methods
    private BigDecimal formatDecimal(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : null;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Map<?, ?> nestedMap) {
            return (Map<String, Object>) nestedMap;
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }
}
