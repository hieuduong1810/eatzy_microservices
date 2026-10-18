package com.eatzy.communication.designpattern.factory;

import org.springframework.stereotype.Component;

@Component
public class OrderNotificationCreator implements INotificationCreator {
    @Override
    public String getType() {
        return NotificationFactory.TYPE_ORDER_STATUS;
    }

    @Override
    public Notification createNotification() {
        return new OrderNotification();
    }
}
