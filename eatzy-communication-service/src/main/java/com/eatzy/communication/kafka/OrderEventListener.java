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
    private final com.eatzy.communication.designpattern.adapter.RestaurantServiceClient restaurantServiceClient;
    private final com.eatzy.communication.service.RedisChatService redisChatService;

    public OrderEventListener(WebSocketService webSocketService, AuthServiceClient authServiceClient, com.eatzy.communication.designpattern.adapter.RestaurantServiceClient restaurantServiceClient, com.eatzy.communication.service.RedisChatService redisChatService) {
        this.webSocketService = webSocketService;
        this.authServiceClient = authServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
        this.redisChatService = redisChatService;
    }

    @KafkaListener(topics = "order-events", groupId = "communication-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvents(@org.springframework.messaging.handler.annotation.Payload Object event) {
        if (event instanceof org.apache.kafka.clients.consumer.ConsumerRecord) {
            event = ((org.apache.kafka.clients.consumer.ConsumerRecord<?, ?>) event).value();
        }
        
        log.info("📥 Received event on order-events topic: {}", event);

        // Convert whatever POJO/Map is received into a Map<String, Object>
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        Map<String, Object> record;
        try {
            record = mapper.convertValue(event, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert event to Map: {}", e.getMessage());
            return;
        }

        try {
            if (!record.containsKey("orderId"))
                return;

            Long orderId = getLongValue(record, "orderId");
            String status = record.containsKey("newStatus") ? (String) record.get("newStatus")
                    : (String) record.get("status");
            Long customerId = getLongValue(record, "customerId");
            Long restaurantId = getLongValue(record, "restaurantId");
            Long driverId = record.containsKey("driverId") ? getLongValue(record, "driverId") : null;

            String message = record.containsKey("message") ? (String) record.get("message")
                    : "Đơn hàng của bạn đã có cập nhật mới";

            // Get emails for routing
            String customerEmail = getEmail(customerId);
            
            // Look up restaurant owner's email
            String restaurantEmail = null;
            if (restaurantId != null) {
                try {
                    Map<String, Object> restData = restaurantServiceClient.getRestaurantById(restaurantId);
                    if (restData != null && restData.containsKey("ownerId")) {
                        Long ownerId = getLongValue(restData, "ownerId");
                        restaurantEmail = getEmail(ownerId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch restaurant owner for restaurantId {}: {}", restaurantId, e.getMessage());
                }
            }

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

            // Clean up chat history upon delivery
            if ("DELIVERED".equalsIgnoreCase(status)) {
                redisChatService.deleteChatHistory(orderId);
            }

        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }

    private String getEmail(Long userId) {
        if (userId == null)
            return null;
        Map<String, Object> userData = authServiceClient.getUserById(userId);
        if (userData != null && userData.containsKey("email")) {
            return (String) userData.get("email");
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof Long)
            return (Long) value;
        if (value instanceof Integer)
            return ((Integer) value).longValue();
        if (value instanceof Number)
            return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
