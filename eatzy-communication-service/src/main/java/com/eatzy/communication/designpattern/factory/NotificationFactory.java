package com.eatzy.communication.designpattern.factory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Factory Method Pattern - Creator.
 * Creates the appropriate Notification subclass based on type.
 * Centralizes notification creation logic instead of scattering "new XxxNotification()" everywhere.
 */
public class NotificationFactory {

    public static final String TYPE_ORDER_STATUS = "ORDER_STATUS";
    public static final String TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String TYPE_DRIVER_LOCATION = "DRIVER_LOCATION";
    public static final String TYPE_SYSTEM = "SYSTEM";

    private NotificationFactory() {
        // Utility class
    }

    /**
     * Factory Method: creates the correct Notification subclass.
     */
    public static Notification create(String type) {
        Notification notification;

        switch (type) {
            case TYPE_ORDER_STATUS:
                notification = new OrderNotification();
                break;
            case TYPE_CHAT_MESSAGE:
                notification = new ChatNotification();
                break;
            case TYPE_DRIVER_LOCATION:
                notification = new DriverLocationNotification();
                break;
            case TYPE_SYSTEM:
                notification = new SystemNotification();
                break;
            default:
                throw new IllegalArgumentException("Unknown notification type: " + type);
        }

        notification.setType(type);
        notification.setTimestamp(Instant.now());
        return notification;
    }

    // ============ Convenience factory methods ============

    /**
     * Create an order notification (NEW_ORDER, ORDER_STATUS_CHANGED, etc.)
     */
    public static OrderNotification createOrderNotification(
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
    public static ChatNotification createChatNotification(
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
    public static DriverLocationNotification createDriverLocationNotification(
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
    public static SystemNotification createSystemNotification(
            String recipientEmail, String title, String message, String severity) {
        SystemNotification n = (SystemNotification) create(TYPE_SYSTEM);
        n.setRecipientEmail(recipientEmail);
        n.setTitle(title);
        n.setMessage(message);
        n.setSeverity(severity);
        return n;
    }
}
