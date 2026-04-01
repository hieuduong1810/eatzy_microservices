package com.eatzy.order.service;

import com.eatzy.common.event.OrderDeliveredEvent;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.domain.Order;
import com.eatzy.order.domain.OrderEarningsSummary;
import com.eatzy.order.kafka.OrderEventProducer;
import com.eatzy.order.repository.OrderEarningsSummaryRepository;
import com.eatzy.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for creating and managing order earnings summaries.
 * Publishes OrderDeliveredEvent for Payment Service (Domain 5) to handle wallet
 * distribution.
 */
@Service
public class OrderEarningsSummaryService {

    private static final Logger log = LoggerFactory.getLogger(OrderEarningsSummaryService.class);

    private final OrderEarningsSummaryRepository earningsSummaryRepository;
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Value("${app.commission.restaurant-rate:0.20}")
    private BigDecimal defaultRestaurantCommissionRate;

    @Value("${app.commission.driver-rate:0.15}")
    private BigDecimal defaultDriverCommissionRate;

    public OrderEarningsSummaryService(OrderEarningsSummaryRepository earningsSummaryRepository,
            OrderRepository orderRepository,
            OrderEventProducer orderEventProducer) {
        this.earningsSummaryRepository = earningsSummaryRepository;
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    @Transactional
    public OrderEarningsSummary createEarningsSummary(Long orderId) throws IdInvalidException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IdInvalidException("Order not found with id: " + orderId));

        // Check if already exists
        Optional<OrderEarningsSummary> existing = earningsSummaryRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;

        // Calculate restaurant commission
        BigDecimal restaurantCommission = subtotal.multiply(defaultRestaurantCommissionRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal restaurantNetEarning = subtotal.subtract(restaurantCommission);

        // Calculate driver commission
        BigDecimal driverCommission = deliveryFee.multiply(defaultDriverCommissionRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal driverNetEarning = deliveryFee.subtract(driverCommission);

        // Platform earning = restaurant commission + driver commission - voucher cost
        BigDecimal platformEarning = restaurantCommission.add(driverCommission).subtract(discountAmount);

        OrderEarningsSummary summary = OrderEarningsSummary.builder()
                .order(order)
                .driverId(order.getDriverId())
                .restaurantId(order.getRestaurantId())
                .orderSubtotal(subtotal)
                .deliveryFee(deliveryFee)
                .restaurantCommissionRate(defaultRestaurantCommissionRate)
                .restaurantCommissionAmount(restaurantCommission)
                .restaurantNetEarning(restaurantNetEarning)
                .driverCommissionRate(defaultDriverCommissionRate)
                .driverCommissionAmount(driverCommission)
                .driverNetEarning(driverNetEarning)
                .platformVoucherCost(discountAmount)
                .platformTotalEarning(platformEarning)
                .recordedAt(Instant.now())
                .build();

        summary = earningsSummaryRepository.save(summary);

        // Publish event for Payment Service to handle wallet distribution
        orderEventProducer.publishOrderDelivered(new OrderDeliveredEvent(
                orderId, order.getCustomerId(), order.getDriverId(), order.getRestaurantId(),
                subtotal, deliveryFee, discountAmount, order.getTotalAmount(),
                driverNetEarning, restaurantNetEarning));

        log.info("✅ Created earnings summary for order {}: restaurant={}, driver={}, platform={}",
                orderId, restaurantNetEarning, driverNetEarning, platformEarning);

        return summary;
    }

    public Optional<OrderEarningsSummary> getByOrderId(Long orderId) {
        return earningsSummaryRepository.findByOrderId(orderId);
    }

    public BigDecimal getDriverTotalEarnings(Long driverId) {
        BigDecimal total = earningsSummaryRepository.sumDriverEarnings(driverId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getRestaurantTotalEarnings(Long restaurantId) {
        BigDecimal total = earningsSummaryRepository.sumRestaurantEarnings(restaurantId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
