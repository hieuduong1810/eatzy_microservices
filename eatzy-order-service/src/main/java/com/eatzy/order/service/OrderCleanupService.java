package com.eatzy.order.service;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.adapter.AuthServiceClient;
import com.eatzy.order.domain.Order;
import com.eatzy.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled cleanup service for stale/expired orders.
 * Ported from monolith OrderCleanupService.
 */
@Service
public class OrderCleanupService {

    private static final Logger log = LoggerFactory.getLogger(OrderCleanupService.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuthServiceClient authServiceClient;

    @Value("${app.cleanup.vnpay-expiry-minutes:15}")
    private int vnpayExpiryMinutes;

    @Value("${app.cleanup.restaurant-timeout-minutes:15}")
    private int restaurantTimeoutMinutes;

    @Value("${app.cleanup.driver-timeout-minutes:30}")
    private int driverTimeoutMinutes;

    @Value("${app.cleanup.driver-accept-timeout-seconds:30}")
    private int driverAcceptTimeoutSeconds;

    public OrderCleanupService(OrderRepository orderRepository,
                               OrderService orderService,
                               AuthServiceClient authServiceClient) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Cleanup expired VNPAY orders that were never paid.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredVNPayOrders() {
        try {
            Instant cutoffTime = Instant.now().minus(vnpayExpiryMinutes, ChronoUnit.MINUTES);
            List<Order> expiredOrders = orderRepository
                    .findByPaymentMethodAndPaymentStatusAndCreatedAtBefore("VNPAY", "UNPAID", cutoffTime);

            if (!expiredOrders.isEmpty()) {
                log.info("Found {} expired VNPAY orders to cleanup", expiredOrders.size());
                for (Order order : expiredOrders) {
                    log.info("Deleting expired VNPAY order: orderId={}, age={}min",
                            order.getId(), ChronoUnit.MINUTES.between(order.getCreatedAt(), Instant.now()));
                    orderRepository.delete(order);
                }
                log.info("Successfully cleaned up {} expired VNPAY orders", expiredOrders.size());
            }
        } catch (Exception e) {
            log.error("Error during VNPAY order cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Auto-cancel PENDING and PREPARING orders that exceed timeout.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCancelStaleOrders() {
        try {
            // PENDING orders: restaurant didn't respond
            Instant restaurantCutoff = Instant.now().minus(restaurantTimeoutMinutes, ChronoUnit.MINUTES);
            List<Order> pendingOrders = orderRepository.findByOrderStatusAndCreatedAtBefore("PENDING", restaurantCutoff);
            for (Order order : pendingOrders) {
                try {
                    orderService.cancelOrder(order.getId(), "Restaurant did not respond in time");
                    log.info("Auto-cancelled PENDING order {}", order.getId());
                } catch (IdInvalidException e) {
                    log.error("Failed to cancel PENDING order {}: {}", order.getId(), e.getMessage());
                }
            }

            // PREPARING orders: no driver found
            Instant driverCutoff = Instant.now().minus(driverTimeoutMinutes, ChronoUnit.MINUTES);
            List<Order> preparingOrders = orderRepository.findByOrderStatusAndPreparingAtBefore("PREPARING", driverCutoff);
            for (Order order : preparingOrders) {
                try {
                    orderService.cancelOrder(order.getId(), "No driver available");
                    log.info("Auto-cancelled PREPARING order {}", order.getId());
                } catch (IdInvalidException e) {
                    log.error("Failed to cancel PREPARING order {}: {}", order.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during auto-cancel cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Auto-accept orders assigned to drivers who haven't responded.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void autoAcceptUnrespondedOrders() {
        try {
            Instant cutoffTime = Instant.now().minus(driverAcceptTimeoutSeconds, ChronoUnit.SECONDS);
            List<Order> unrespondedOrders = orderRepository
                    .findByOrderStatusAndDriverIdIsNotNullAndAssignedAtBefore("PREPARING", cutoffTime);

            for (Order order : unrespondedOrders) {
                try {
                    orderService.acceptOrderByDriver(order.getId(), order.getDriverId());
                    log.info("✅ Auto-accepted order {} for driver {}", order.getId(), order.getDriverId());
                } catch (IdInvalidException e) {
                    log.error("Failed to auto-accept order {}: {}", order.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during auto-accept cleanup: {}", e.getMessage(), e);
        }
    }
}
