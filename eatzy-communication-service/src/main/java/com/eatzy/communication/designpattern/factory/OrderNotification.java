package com.eatzy.communication.designpattern.factory;

import lombok.*;

/**
 * Factory Method Pattern - Concrete Product: Order Notification.
 * Delivers order lifecycle updates (new order, status change, delivery).
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderNotification extends Notification {
    private Long orderId;
    private String orderStatus;

    @Override
    public String getDestination() {
        return "/queue/orders";
    }
}
