package com.eatzy.communication.designpattern.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Method Pattern - Creator (Refactored to use Spring Map Injection).
 * Creates the appropriate Notification subclass based on type.
 * Centralizes notification creation logic instead of scattering "new XxxNotification()" everywhere.
 * Completely eliminates switch-case by using INotificationCreator registry.
 */
@Component
public class NotificationFactory {

    public static final String TYPE_ORDER_STATUS = "ORDER_STATUS";
    public static final String TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String TYPE_DRIVER_LOCATION = "DRIVER_LOCATION";
    public static final String TYPE_SYSTEM = "SYSTEM";

    private final Map<String, INotificationCreator> creatorRegistry = new HashMap<>();

    @Autowired
    public NotificationFactory(List<INotificationCreator> creators) {
        for (INotificationCreator creator : creators) {
            creatorRegistry.put(creator.getType(), creator);
        }
    }

    /**
     * Factory Method: creates the correct Notification subclass using the registry.
     */
    public Notification create(String type) {
        INotificationCreator creator = creatorRegistry.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("Unknown notification type: " + type);
        }

        Notification notification = creator.createNotification();
        notification.setType(type);
        notification.setTimestamp(Instant.now());
        return notification;
    }

    // ============ Convenience factory methods ============

    /**
     * Create an order notification (NEW_ORDER, ORDER_STATUS_CHANGED, etc.)
     */
    public OrderNotification createOrderNotification(
            String recipientEmail, Long orderId, String orderStatus, String message, Object data) {
        OrderNotification n = (OrderNotification) create(TYPE_ORDER_STATUS);
        n.setRecipientEmail(recipientEmail);
        n.setOrderId(orderId);
        n.setOrderStatus(orderStatus);
        n.setMessage(message);
        n.setData(data);
        return n;
    }

    /**
     * Create a chat notification
     */
    public ChatNotification createChatNotification(
            String recipientEmail, Long orderId, Long senderId, String senderName,
            String senderType, String message) {
        ChatNotification n = (ChatNotification) create(TYPE_CHAT_MESSAGE);
        n.setRecipientEmail(recipientEmail);
        n.setOrderId(orderId);
        n.setSenderId(senderId);
        n.setSenderName(senderName);
        n.setSenderType(senderType);
        n.setMessage(message);
        return n;
    }

    /**
     * Create a driver location notification
     */
    public DriverLocationNotification createDriverLocationNotification(
            String recipientEmail, BigDecimal latitude, BigDecimal longitude) {
        DriverLocationNotification n = (DriverLocationNotification) create(TYPE_DRIVER_LOCATION);
        n.setRecipientEmail(recipientEmail);
        n.setLatitude(latitude);
        n.setLongitude(longitude);
        n.setMessage("Driver location updated");
        return n;
    }

    /**
     * Create a system notification
     */
    public SystemNotification createSystemNotification(
            String recipientEmail, String title, String message, String severity) {
        SystemNotification n = (SystemNotification) create(TYPE_SYSTEM);
        n.setRecipientEmail(recipientEmail);
        n.setTitle(title);
        n.setMessage(message);
        n.setSeverity(severity);
        return n;
    }
}
