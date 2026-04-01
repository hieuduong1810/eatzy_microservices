package com.eatzy.restaurant.designpattern.observer;

import com.eatzy.restaurant.domain.Restaurant;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * ★ DESIGN PATTERN #3: Observer Pattern (Event-Driven)
 * 
 * Event duoc phat ra khi nha hang duoc admin phe duyet (approve).
 * Cac Observer (Listener) lang nghe event nay va tu dong thuc hien hanh dong:
 *   - Gui email chuc mung chu nha hang
 *   - Ghi log audit
 *   - Cap nhat bang xep hang
 * 
 * Subject (Nguoi phat event): RestaurantService
 * Observer (Nguoi nghe event): RestaurantApprovedListener
 */
@Getter
public class RestaurantApprovedEvent extends ApplicationEvent {
    private final Restaurant restaurant;

    public RestaurantApprovedEvent(Object source, Restaurant restaurant) {
        super(source);
        this.restaurant = restaurant;
    }
}
