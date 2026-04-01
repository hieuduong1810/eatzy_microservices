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
    public void handleUserEvents(Map<String, Object> record) {
        log.info("📥 Received event on user-events topic: {}", record);
        
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
