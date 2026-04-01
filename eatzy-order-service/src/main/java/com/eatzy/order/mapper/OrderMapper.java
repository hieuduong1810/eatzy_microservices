package com.eatzy.order.mapper;

import com.eatzy.order.designpattern.adapter.AuthServiceClient;
import com.eatzy.order.designpattern.adapter.RestaurantServiceClient;
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
 * Mapper: Converts Order entities to DTOs, enriching with data from external services via Adapter pattern.
 */
@Component
public class OrderMapper {

    private static final Logger log = LoggerFactory.getLogger(OrderMapper.class);

    private final AuthServiceClient authServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final OrderEarningsSummaryRepository orderEarningsSummaryRepository;

    public OrderMapper(AuthServiceClient authServiceClient,
                       RestaurantServiceClient restaurantServiceClient,
                       OrderEarningsSummaryRepository orderEarningsSummaryRepository) {
        this.authServiceClient = authServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
        this.orderEarningsSummaryRepository = orderEarningsSummaryRepository;
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
                    customer.setPhoneNumber(getStringValue(userData, "phoneNumber"));
                    dto.setCustomer(customer);
                }
            } catch (Exception e) {
                log.warn("Failed to enrich customer info for order {}: {}", order.getId(), e.getMessage());
                dto.setCustomer(new ResOrderDTO.Customer(order.getCustomerId(), null, null));
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

                    // Calculate distance
                    RestaurantServiceClient.DistanceRes distanceRes = restaurantServiceClient.getDrivingDistance(
                            restaurant.getLatitude(), restaurant.getLongitude(),
                            order.getDeliveryLatitude(), order.getDeliveryLongitude());
                    BigDecimal distance = distanceRes != null ? distanceRes.getDistance() : null;
                    dto.setDistance(formatDecimal(distance));
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
                if (driverData != null) {
                    ResOrderDTO.Driver driver = new ResOrderDTO.Driver();
                    driver.setId(order.getDriverId());
                    driver.setName(getStringValue(driverData, "name"));
                    driver.setPhoneNumber(getStringValue(driverData, "phoneNumber"));
                    if (driverProfile != null) {
                        driver.setVehicleType(getStringValue(driverProfile, "vehicleType"));
                        driver.setAverageRating(getStringValue(driverProfile, "averageRating"));
                        driver.setCompletedTrips(getStringValue(driverProfile, "completedTrips"));
                        driver.setVehicleLicensePlate(getStringValue(driverProfile, "vehicleLicensePlate"));
                        driver.setVehicleDetails(getStringValue(driverProfile, "vehicleDetails"));
                    }
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
                Map<String, Object> menuOptionData = restaurantServiceClient.getMenuOptionById(option.getMenuOptionId());
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
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }
}
