package com.eatzy.order.kafka;

import com.eatzy.common.event.UserStatusChangedEvent;
import com.eatzy.order.domain.Order;
import com.eatzy.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Observer/Pub-Sub Pattern: Listens for events from other services.
 * When a user is deactivated, auto-cancel their pending orders.
 */
@Service
public class OrderMessageListener {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageListener.class);

    private final OrderRepository orderRepository;

    public OrderMessageListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
}
