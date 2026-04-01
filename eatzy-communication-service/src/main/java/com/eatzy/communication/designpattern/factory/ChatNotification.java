package com.eatzy.communication.designpattern.factory;

import lombok.*;

/**
 * Factory Method Pattern - Concrete Product: Chat Notification.
 * Delivers chat messages between driver and customer for a specific order.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatNotification extends Notification {
    private Long orderId;
    private Long senderId;
    private String senderName;
    private String senderType; // "DRIVER" or "CUSTOMER"

    @Override
    public String getDestination() {
        return "/queue/chat/order/" + orderId;
    }
}
