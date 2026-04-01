package com.eatzy.communication.service;

import com.eatzy.communication.designpattern.factory.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket service for pushing notifications to clients.
 * Integrates with the Factory Method pattern's Notification objects.
 */
@Service
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Push a Notification (created by Factory Method) to its defined destination.
     * Uses user-specific routing (/user/{email}/{destination}).
     */
    public void pushNotification(Notification notification) {
        if (notification == null || notification.getRecipientEmail() == null) {
            log.warn("Cannot send notification - notification or recipient email is null");
            return;
        }

        String destination = notification.getDestination();
        messagingTemplate.convertAndSendToUser(notification.getRecipientEmail(), destination, notification);
        log.info("📤 Pushed {} notification to user {} at /user/{}{}", 
                notification.getType(), notification.getRecipientEmail(), notification.getRecipientEmail(), destination);
    }
    
    /**
     * Raw send for specific use cases
     */
    public void sendToUser(String email, String destination, Object payload) {
        if (email == null || email.isEmpty()) {
            log.warn("Cannot send message - email is null or empty");
            return;
        }
        messagingTemplate.convertAndSendToUser(email, destination, payload);
    }
}
