package com.eatzy.communication.designpattern.factory;

import org.springframework.stereotype.Component;

@Component
public class DriverLocationNotificationCreator implements INotificationCreator {
    @Override
    public String getType() {
        return NotificationFactory.TYPE_DRIVER_LOCATION;
    }

    @Override
    public Notification createNotification() {
        return new DriverLocationNotification();
    }
}
