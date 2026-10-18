package com.eatzy.communication.designpattern.factory;

import org.springframework.stereotype.Component;

@Component
public class ChatNotificationCreator implements INotificationCreator {
    @Override
    public String getType() {
        return NotificationFactory.TYPE_CHAT_MESSAGE;
    }

    @Override
    public Notification createNotification() {
        return new ChatNotification();
    }
}
