package com.eatzy.restaurant.service;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.restaurant.client.InteractionServiceClient;
import com.eatzy.restaurant.client.OrderServiceClient;
import com.eatzy.restaurant.client.dto.OrderClientDTO;
import com.eatzy.restaurant.client.dto.ReviewClientDTO;
import com.eatzy.restaurant.domain.Dish;
import com.eatzy.restaurant.domain.res.report.*;
import com.eatzy.restaurant.repository.DishRepository;
import com.eatzy.restaurant.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RestaurantReportService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantReportService.class);

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final OrderServiceClient orderServiceClient;
    private final InteractionServiceClient interactionServiceClient;

    public RestaurantReportService(RestaurantRepository restaurantRepository,
            DishRepository dishRepository,
            OrderServiceClient orderServiceClient,
            InteractionServiceClient interactionServiceClient) {
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.orderServiceClient = orderServiceClient;
        this.interactionServiceClient = interactionServiceClient;
    }

    public FullReportDTO getFullReport(Long restaurantId, Instant startDate, Instant endDate)
            throws IdInvalidException {
        validateRestaurant(restaurantId);

        List<OrderClientDTO> orders = orderServiceClient.getOrdersByRestaurantAndDateRange(restaurantId, startDate,
                endDate);
        List<ReviewClientDTO> reviews = getRestaurantReviews(restaurantId, startDate, endDate);

        FullReportDTO report = new FullReportDTO();

        // Revenue & Orders
        calculateRevenueAndOrders(orders, report);

        // Reviews
        calculateReviews(reviews, report);

        // Charts
        report.setRevenueChart(generateRevenueChart(orders, startDate, endDate));
        report.setOrderStatusBreakdown(generateOrderStatusBreakdown(orders));

        // Top Dish
        report.setTopPerformingDish(calculateTopDish(orders));

        return report;
    }

    public List<RevenueReportItemDTO> getRevenueReport(Long restaurantId, Instant startDate, Instant endDate)
            throws IdInvalidException {
        validateRestaurant(restaurantId);
        List<OrderClientDTO> orders = orderServiceClient.getOrdersByRestaurantAndDateRange(restaurantId, startDate,
                endDate);
        return generateRevenueChart(orders, startDate, endDate);
    }

    public List<OrderReportItemDTO> getOrdersReport(Long restaurantId, Instant startDate, Instant endDate)
            throws IdInvalidException {
        validateRestaurant(restaurantId);
        List<OrderClientDTO> orders = orderServiceClient.getOrdersByRestaurantAndDateRange(restaurantId, startDate,
                endDate);

        return orders.stream().map(order -> {
            OrderReportItemDTO item = new OrderReportItemDTO();
            item.setId(order.getId());
            item.setOrderCode("EZ" + order.getId());
            if (order.getCustomer() != null) {
                item.setCustomerName(order.getCustomer().getName());
                item.setCustomerPhone(order.getCustomer().getPhoneNumber());
            }
            item.setOrderTime(order.getCreatedAt());
            item.setDeliveredTime(order.getDeliveredAt());
            item.setStatus(order.getOrderStatus());
            item.setPaymentMethod(order.getPaymentMethod());
            item.setPaymentStatus(order.getPaymentStatus());
            item.setSubtotal(order.getSubtotal());
            item.setDeliveryFee(order.getDeliveryFee());
            item.setDiscountAmount(order.getDiscountAmount());
            item.setTotalAmount(order.getTotalAmount());

            int itemsCount = 0;
            if (order.getOrderItems() != null) {
                itemsCount = order.getOrderItems().stream().mapToInt(OrderClientDTO.OrderItem::getQuantity).sum();
            }
            item.setItemsCount(itemsCount);
            item.setCancellationReason(order.getCancellationReason());
            return item;
        }).collect(Collectors.toList());
    }

    public MenuSummaryDTO getMenuReport(Long restaurantId, Instant startDate, Instant endDate)
            throws IdInvalidException {
        validateRestaurant(restaurantId);
        List<OrderClientDTO> orders = orderServiceClient.getOrdersByRestaurantAndDateRange(restaurantId, startDate,
                endDate);
        List<Dish> allDishes = dishRepository.findByRestaurantId(restaurantId);
        List<ReviewClientDTO> reviews = getRestaurantReviews(restaurantId, null, null); // Get all reviews to calc dish
                                                                                        // ratings

        MenuSummaryDTO summary = new MenuSummaryDTO();
        summary.setTotalDishes(allDishes.size());
        summary.setActiveDishes((int) allDishes.stream().filter(d -> d.getAvailabilityQuantity() > 0).count());
        summary.setOutOfStockDishes((int) allDishes.stream().filter(d -> d.getAvailabilityQuantity() <= 0).count());

        Map<Long, Integer> dishOrderCounts = new HashMap<>();
        Map<Long, BigDecimal> dishRevenue = new HashMap<>();

        for (OrderClientDTO order : orders) {
            if ("DELIVERED".equals(order.getOrderStatus()) && order.getOrderItems() != null) {
                for (OrderClientDTO.OrderItem item : order.getOrderItems()) {
                    Long dishId = item.getDishId();
                    if (dishId == null)
                        continue;

                    dishOrderCounts.put(dishId, dishOrderCounts.getOrDefault(dishId, 0) + item.getQuantity());
                    BigDecimal revenue = item.getPriceAtPurchase() != null ? item.getPriceAtPurchase()
                            : BigDecimal.ZERO;
                    dishRevenue.put(dishId, dishRevenue.getOrDefault(dishId, BigDecimal.ZERO).add(revenue));
                }
            }
        }

        List<MenuAnalyticsItemDTO> analyticsItems = new ArrayList<>();
        for (Dish dish : allDishes) {
            MenuAnalyticsItemDTO item = new MenuAnalyticsItemDTO();
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setCategoryName(dish.getCategory() != null ? dish.getCategory().getName() : "Không phân loại");
            item.setImageUrl(dish.getImageUrl());
            item.setPrice(dish.getPrice());

            item.setTotalOrdered(dishOrderCounts.getOrDefault(dish.getId(), 0));
            item.setTotalRevenue(dishRevenue.getOrDefault(dish.getId(), BigDecimal.ZERO));

            // Calc average rating
            List<ReviewClientDTO> dishReviews = reviews.stream()
                    .filter(r -> "DISH".equals(r.getReviewTarget())
                            && String.valueOf(dish.getId()).equals(r.getTargetName()))
                    .collect(Collectors.toList());

            if (dishReviews.isEmpty()) {
                item.setAverageRating(BigDecimal.ZERO);
                item.setReviewCount(0);
            } else {
                double avg = dishReviews.stream().mapToInt(ReviewClientDTO::getRating).average().orElse(0);
                item.setAverageRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
                item.setReviewCount(dishReviews.size());
            }

            item.setTrend("stable");
            item.setTrendPercent(BigDecimal.ZERO);

            analyticsItems.add(item);
        }

        analyticsItems.sort((a, b) -> b.getTotalOrdered().compareTo(a.getTotalOrdered()));

        summary.setTopSellingDishes(analyticsItems.stream().limit(5).collect(Collectors.toList()));
        summary.setLowPerformingDishes(
                analyticsItems.stream().filter(a -> a.getTotalOrdered() < 5).limit(5).collect(Collectors.toList()));

        // Category Breakdown
        Map<String, CategoryAnalyticsItemDTO> categoryMap = new HashMap<>();
        for (MenuAnalyticsItemDTO item : analyticsItems) {
            String catName = item.getCategoryName();
            CategoryAnalyticsItemDTO cat = categoryMap.computeIfAbsent(catName, k -> {
                CategoryAnalyticsItemDTO c = new CategoryAnalyticsItemDTO();
                c.setCategoryName(k);
                c.setTotalDishes(0);
                c.setTotalOrdered(0);
                c.setTotalRevenue(BigDecimal.ZERO);
                return c;
            });
            cat.setTotalDishes(cat.getTotalDishes() + 1);
            cat.setTotalOrdered(cat.getTotalOrdered() + item.getTotalOrdered());
            cat.setTotalRevenue(cat.getTotalRevenue().add(item.getTotalRevenue()));
        }

        BigDecimal totalMenuRevenue = categoryMap.values().stream()
                .map(CategoryAnalyticsItemDTO::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryAnalyticsItemDTO> catList = new ArrayList<>(categoryMap.values());
        for (CategoryAnalyticsItemDTO cat : catList) {
            if (totalMenuRevenue.compareTo(BigDecimal.ZERO) > 0) {
                cat.setPercentOfTotal(cat.getTotalRevenue()
                        .multiply(new BigDecimal(100))
                        .divide(totalMenuRevenue, 1, RoundingMode.HALF_UP));
            } else {
                cat.setPercentOfTotal(BigDecimal.ZERO);
            }
        }
        summary.setCategoryBreakdown(catList);

        return summary;
    }

    public ReviewSummaryDTO getReviewReport(Long restaurantId, Instant startDate, Instant endDate)
            throws IdInvalidException {
        validateRestaurant(restaurantId);
        List<ReviewClientDTO> reviews = getRestaurantReviews(restaurantId, startDate, endDate);

        ReviewSummaryDTO summary = new ReviewSummaryDTO();
        summary.setTotalReviews(reviews.size());

        if (reviews.isEmpty()) {
            summary.setAverageRating(BigDecimal.ZERO);
            summary.setRatingDistribution(new RatingDistributionDTO(0, 0, 0, 0, 0));
            summary.setRecentReviews(Collections.emptyList());
            summary.setResponseRate(BigDecimal.ZERO);
            summary.setAverageResponseTime(BigDecimal.ZERO);
            return summary;
        }

        double avg = reviews.stream().mapToInt(ReviewClientDTO::getRating).average().orElse(0);
        summary.setAverageRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));

        int[] stars = new int[6];
        int repliedCount = 0;

        for (ReviewClientDTO r : reviews) {
            if (r.getRating() >= 1 && r.getRating() <= 5) {
                stars[r.getRating()]++;
            }
            if (r.getReply() != null && !r.getReply().trim().isEmpty()) {
                repliedCount++;
            }
        }

        summary.setRatingDistribution(new RatingDistributionDTO(stars[1], stars[2], stars[3], stars[4], stars[5]));

        summary.setResponseRate(BigDecimal.valueOf((double) repliedCount * 100 / reviews.size())
                .setScale(1, RoundingMode.HALF_UP));

        // Average response time not available easily, mock it or leave 0
        summary.setAverageResponseTime(BigDecimal.ZERO);

        List<ReviewReportItemDTO> recent = reviews.stream()
                .sorted(Comparator.comparing(ReviewClientDTO::getCreatedAt).reversed())
                .limit(10)
                .map(r -> {
                    ReviewReportItemDTO item = new ReviewReportItemDTO();
                    item.setId(r.getId());
                    if (r.getOrder() != null) {
                        item.setOrderId(r.getOrder().getId());
                        item.setOrderCode("EZ" + r.getOrder().getId());
                    }
                    if (r.getCustomer() != null) {
                        item.setCustomerName(r.getCustomer().getName());
                    }
                    item.setRating(r.getRating());
                    item.setComment(r.getComment());
                    item.setReply(r.getReply());
                    item.setCreatedAt(r.getCreatedAt());
                    // Dish names not easily available from Review directly without order items
                    item.setDishNames(Collections.emptyList());
                    return item;
                }).collect(Collectors.toList());

        summary.setRecentReviews(recent);
        return summary;
    }

    private void calculateRevenueAndOrders(List<OrderClientDTO> orders, FullReportDTO report) {
        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal netRev = BigDecimal.ZERO;
        int completed = 0;
        int cancelled = 0;

        for (OrderClientDTO order : orders) {
            if ("DELIVERED".equals(order.getOrderStatus())) {
                completed++;
                totalRev = totalRev.add(order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO);
                netRev = netRev.add(
                        order.getRestaurantNetEarning() != null ? order.getRestaurantNetEarning() : BigDecimal.ZERO);
            } else if ("REJECTED".equals(order.getOrderStatus()) || "CANCELLED".equals(order.getOrderStatus())) {
                cancelled++;
            }
        }

        report.setTotalOrders(orders.size());
        report.setCompletedOrders(completed);
        report.setCancelledOrders(cancelled);
        report.setTotalRevenue(totalRev);
        report.setNetRevenue(netRev);

        if (orders.isEmpty()) {
            report.setCancelRate(BigDecimal.ZERO);
            report.setAverageOrderValue(BigDecimal.ZERO);
        } else {
            report.setCancelRate(
                    BigDecimal.valueOf((double) cancelled * 100 / orders.size()).setScale(1, RoundingMode.HALF_UP));
            if (completed > 0) {
                report.setAverageOrderValue(totalRev.divide(new BigDecimal(completed), 0, RoundingMode.HALF_UP));
            } else {
                report.setAverageOrderValue(BigDecimal.ZERO);
            }
        }
    }

    private void calculateReviews(List<ReviewClientDTO> reviews, FullReportDTO report) {
        report.setTotalReviews(reviews.size());
        if (reviews.isEmpty()) {
            report.setAverageRating(BigDecimal.ZERO);
        } else {
            double avg = reviews.stream().mapToInt(ReviewClientDTO::getRating).average().orElse(0);
            report.setAverageRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }
    }

    private List<RevenueReportItemDTO> generateRevenueChart(List<OrderClientDTO> orders, Instant startDate,
            Instant endDate) {
        if (startDate == null || endDate == null)
            return Collections.emptyList();

        LocalDate start = startDate.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = endDate.atZone(ZoneId.systemDefault()).toLocalDate();

        Map<LocalDate, RevenueReportItemDTO> map = new TreeMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            map.put(date, RevenueReportItemDTO.builder()
                    .date(date)
                    .foodRevenue(BigDecimal.ZERO)
                    .deliveryFee(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .commissionAmount(BigDecimal.ZERO)
                    .netRevenue(BigDecimal.ZERO)
                    .totalOrders(0)
                    .build());
        }

        for (OrderClientDTO order : orders) {
            if ("DELIVERED".equals(order.getOrderStatus())) {
                LocalDate orderDate = order.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                RevenueReportItemDTO item = map.get(orderDate);
                if (item != null) {
                    item.setFoodRevenue(item.getFoodRevenue()
                            .add(order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO));
                    item.setDeliveryFee(item.getDeliveryFee()
                            .add(order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO));
                    item.setDiscountAmount(item.getDiscountAmount()
                            .add(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO));
                    item.setCommissionAmount(item.getCommissionAmount()
                            .add(order.getRestaurantCommissionAmount() != null ? order.getRestaurantCommissionAmount()
                                    : BigDecimal.ZERO));
                    item.setNetRevenue(item.getNetRevenue()
                            .add(order.getRestaurantNetEarning() != null ? order.getRestaurantNetEarning()
                                    : BigDecimal.ZERO));
                    item.setTotalOrders(item.getTotalOrders() + 1);
                }
            }
        }

        return new ArrayList<>(map.values());
    }

    private List<OrderStatusBreakdownDTO> generateOrderStatusBreakdown(List<OrderClientDTO> orders) {
        if (orders.isEmpty())
            return Collections.emptyList();

        Map<String, Integer> counts = new HashMap<>();
        for (OrderClientDTO order : orders) {
            String status = order.getOrderStatus() != null ? order.getOrderStatus() : "UNKNOWN";
            counts.put(status, counts.getOrDefault(status, 0) + 1);
        }

        List<OrderStatusBreakdownDTO> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            OrderStatusBreakdownDTO dto = new OrderStatusBreakdownDTO();
            dto.setStatus(entry.getKey());
            dto.setCount(entry.getValue());
            dto.setPercent(BigDecimal.valueOf((double) entry.getValue() * 100 / orders.size()).setScale(1,
                    RoundingMode.HALF_UP));
            list.add(dto);
        }
        return list;
    }

    private String calculateTopDish(List<OrderClientDTO> orders) {
        Map<Long, Integer> dishCounts = new HashMap<>();
        for (OrderClientDTO order : orders) {
            if ("DELIVERED".equals(order.getOrderStatus()) && order.getOrderItems() != null) {
                for (OrderClientDTO.OrderItem item : order.getOrderItems()) {
                    if (item.getDishId() != null) {
                        dishCounts.put(item.getDishId(),
                                dishCounts.getOrDefault(item.getDishId(), 0) + item.getQuantity());
                    }
                }
            }
        }

        if (dishCounts.isEmpty())
            return "N/A";

        Long topDishId = Collections.max(dishCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
        Optional<Dish> dish = dishRepository.findById(topDishId);
        return dish.map(Dish::getName).orElse("Unknown Dish");
    }

    private void validateRestaurant(Long id) throws IdInvalidException {
        if (!restaurantRepository.existsById(id)) {
            throw new IdInvalidException("Restaurant not found");
        }
    }

    private List<ReviewClientDTO> getRestaurantReviews(Long restaurantId, Instant startDate, Instant endDate) {
        try {
            List<ReviewClientDTO> reviews = interactionServiceClient.getReviewsByTarget("RESTAURANT",
                    String.valueOf(restaurantId));
            if (reviews == null)
                return Collections.emptyList();

            if (startDate != null && endDate != null) {
                return reviews.stream()
                        .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(startDate)
                                && !r.getCreatedAt().isAfter(endDate))
                        .collect(Collectors.toList());
            }
            return reviews;
        } catch (Exception e) {
            log.warn("Failed to fetch reviews: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
