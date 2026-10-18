package com.eatzy.communication.designpattern.factory;

/**
 * Factory Method Pattern - Abstract Creator Interface.
 * Used for Strategy Map Injection to avoid switch-case.
 */
public interface INotificationCreator {
    String getType();
    Notification createNotification();
}
