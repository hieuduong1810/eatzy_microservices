package com.eatzy.restaurant.designpattern.factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * ★ DESIGN PATTERN #2: Factory Method Pattern
 * 
 * NotificationFactory - lop chuyen tao ra Notifier phu hop dua tren kenh gui.
 * 
 * Cach dung:
 *   Notifier notifier = notificationFactory.getNotifier("EMAIL");
 *   notifier.send("user@email.com", "Subject", "Body");
 * 
 * Khi can them kenh moi (VD: Zalo), chi can tao class ZaloNotifier implements Notifier,
 * Factory se tu dong nhan dien va dang ky ma KHONG CAN SUA bat ky dong code nao trong Factory.
 */
@Component
public class NotificationFactory {

    private final Map<String, Notifier> notifierMap;

    /**
     * Spring tu dong inject tat ca cac bean implements Notifier vao day.
     * Factory dung channel name lam key de tra ve dung Notifier khi can.
     */
    public NotificationFactory(List<Notifier> notifiers) {
        this.notifierMap = notifiers.stream()
                .collect(Collectors.toMap(Notifier::getChannel, Function.identity()));
    }

    /**
     * Tra ve Notifier tuong ung voi kenh gui.
     * 
     * @param channel Ten kenh gui: "EMAIL", "SMS", "PUSH"
     * @return Notifier tuong ung
     * @throws IllegalArgumentException Neu kenh gui khong ton tai
     */
    public Notifier getNotifier(String channel) {
        Notifier notifier = notifierMap.get(channel.toUpperCase());
        if (notifier == null) {
            throw new IllegalArgumentException("Unsupported notification channel: " + channel
                    + ". Available channels: " + notifierMap.keySet());
        }
        return notifier;
    }
}
