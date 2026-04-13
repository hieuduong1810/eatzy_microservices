package com.eatzy.communication.kafka;

import com.eatzy.communication.designpattern.factory.NotificationFactory;
import com.eatzy.communication.designpattern.factory.SystemNotification;
import com.eatzy.communication.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Observer Pattern - Kafka Consumer for user events.
 */
@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final WebSocketService webSocketService;

    public UserEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @KafkaListener(topics = "user-events", groupId = "communication-group")
    public void handleUserEvents(@org.springframework.messaging.handler.annotation.Payload Object event) {
        if (event instanceof org.apache.kafka.clients.consumer.ConsumerRecord) {
            event = ((org.apache.kafka.clients.consumer.ConsumerRecord<?, ?>) event).value();
        }
        
        log.info("📥 Received event on user-events topic: {}", event);
        
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
            if (record.containsKey("isActive") && Boolean.FALSE.equals(record.get("isActive"))) {
                // User deactivated -> send system notification requiring them to log out or indicating account closure
                String email = (String) record.get("email");
                if (email != null) {
                    SystemNotification notification = NotificationFactory.createSystemNotification(
                            email, "Account Deactivated", "Your account has been deactivated. Please contact support.", "ERROR"
                    );
                    webSocketService.pushNotification(notification);
                }
            }
        } catch (Exception e) {
            log.error("Error processing user event: {}", e.getMessage(), e);
        }
    }
}
