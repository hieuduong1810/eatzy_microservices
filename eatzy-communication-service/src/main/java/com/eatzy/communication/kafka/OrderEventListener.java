package com.eatzy.communication.kafka;

import com.eatzy.common.event.OrderCreatedEvent;
import com.eatzy.common.event.OrderDeliveredEvent;
import com.eatzy.common.event.OrderStatusChangedEvent;
import com.eatzy.communication.designpattern.adapter.AuthServiceClient;
import com.eatzy.communication.designpattern.factory.NotificationFactory;
import com.eatzy.communication.designpattern.factory.OrderNotification;
import com.eatzy.communication.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Observer Pattern - Kafka Consumer for order lifecycle events.
 * Pushes WebSocket notifications to relevant users using the Factory Method.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final WebSocketService webSocketService;
    private final AuthServiceClient authServiceClient;

    public OrderEventListener(WebSocketService webSocketService, AuthServiceClient authServiceClient) {
        this.webSocketService = webSocketService;
        this.authServiceClient = authServiceClient;
    }

    @KafkaListener(topics = "order-events", groupId = "communication-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvents(Map<String, Object> record) {
        log.info("📥 Received event on order-events topic: {}", record);
        
        // This is a simplified dispatcher. In a real scenario with proper type mappings in Kafka,
        // we'd receive the specific event objects. For now, we'll try to extract fields manually
        // or configure Kafka to deserialize objects correctly based on headers.
        // Assuming JsonDeserializer converts to Map for untyped consumption, or we can use custom mapping.
        
        try {
            if (!record.containsKey("orderId")) return;
            
            Long orderId = getLongValue(record, "orderId");
            String status = record.containsKey("newStatus") ? (String) record.get("newStatus") : (String) record.get("status");
            Long customerId = getLongValue(record, "customerId");
            Long restaurantId = getLongValue(record, "restaurantId");
            Long driverId = record.containsKey("driverId") ? getLongValue(record, "driverId") : null;
            
            String message = record.containsKey("message") ? (String) record.get("message") : "Đơn hàng của bạn đã có cập nhật mới";

            // Get emails for routing
            String customerEmail = getEmail(customerId);
            String restaurantEmail = getEmail(restaurantId);
            String driverEmail = getEmail(driverId);
            
            // Push to customer
            if (customerEmail != null) {
                OrderNotification notification = NotificationFactory.createOrderNotification(
                        customerEmail, orderId, status, message, record);
                webSocketService.pushNotification(notification);
            }
            
            // Push to restaurant
            if (restaurantEmail != null) {
                OrderNotification notification = NotificationFactory.createOrderNotification(
                        restaurantEmail, orderId, status, message, record);
                webSocketService.pushNotification(notification);
            }
            
            // Push to driver
            if (driverEmail != null) {
                OrderNotification notification = NotificationFactory.createOrderNotification(
                        driverEmail, orderId, status, message, record);
                webSocketService.pushNotification(notification);
            }
            
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }

    private String getEmail(Long userId) {
        if (userId == null) return null;
        Map<String, Object> userData = authServiceClient.getUserById(userId);
        if (userData != null && userData.containsKey("email")) {
            return (String) userData.get("email");
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
