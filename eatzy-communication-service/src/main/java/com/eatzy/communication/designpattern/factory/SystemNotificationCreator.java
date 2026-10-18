package com.eatzy.communication.designpattern.factory;

import org.springframework.stereotype.Component;

@Component
public class SystemNotificationCreator implements INotificationCreator {
    @Override
    public String getType() {
        return NotificationFactory.TYPE_SYSTEM;
    }

    @Override
    public Notification createNotification() {
        return new SystemNotification();
    }
}
