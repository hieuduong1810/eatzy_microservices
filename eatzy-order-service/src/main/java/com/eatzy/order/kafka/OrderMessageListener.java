package com.eatzy.order.kafka;

import com.eatzy.common.event.DriverOnlineEvent;
import com.eatzy.common.event.UserStatusChangedEvent;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.domain.Order;
import com.eatzy.order.repository.OrderRepository;
import com.eatzy.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Observer/Pub-Sub Pattern: Listens for events from other services.
 * - UserStatusChangedEvent: When user deactivated, auto-cancel pending orders.
 * - DriverOnlineEvent: When driver goes online, assign waiting orders.
 */
@Service
public class OrderMessageListener {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageListener.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderMessageListener(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @KafkaListener(topics = "user-events", groupId = "order-group")
    @Transactional
    public void handleUserStatusChangedEvent(UserStatusChangedEvent event) {
        log.info("📥 Received UserStatusChangedEvent: {}", event);

        // If user is deactivated, cancel their pending orders
        if (Boolean.FALSE.equals(event.getIsActive())) {
            // Cancel pending orders where user is customer
            List<Order> pendingOrders = orderRepository.findByCustomerIdAndOrderStatus(
                    event.getUserId(), "PENDING");
            for (Order order : pendingOrders) {
                order.setOrderStatus("REJECTED");
                order.setCancellationReason("Customer account deactivated");
                orderRepository.save(order);
                log.info("Auto-cancelled order {} due to customer {} deactivation",
                        order.getId(), event.getUserId());
            }

            // If user is a driver, unassign from preparing orders
            if ("DRIVER".equals(event.getRole())) {
                List<Order> driverOrders = orderRepository.findByDriverIdAndOrderStatus(
                        event.getUserId(), "PREPARING");
                for (Order order : driverOrders) {
                    order.setDriverId(null);
                    order.setAssignedAt(null);
                    orderRepository.save(order);
                    log.info("Unassigned deactivated driver {} from order {}",
                            event.getUserId(), order.getId());
                }
            }
        }
    }

    /**
     * When a driver goes online, find PREPARING orders without a driver and try to assign them.
     * Matches eatzy_backend: goOnline triggers search for unassigned PREPARING orders.
     */
    @KafkaListener(topics = "driver-events", groupId = "order-group")
    public void handleDriverOnlineEvent(DriverOnlineEvent event) {
        log.info("📥 Received DriverOnlineEvent: driver {} at ({}, {})",
                event.getDriverId(), event.getLatitude(), event.getLongitude());

        try {
            orderService.findAndAssignNextOrderForSpecificDriver(event.getDriverId(), event.getLatitude(), event.getLongitude());
        } catch (Exception e) {
            log.error("Error handling DriverOnlineEvent for driver {}: {}",
                    event.getDriverId(), e.getMessage(), e);
        }
    }
}

