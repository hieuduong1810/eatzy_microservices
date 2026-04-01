package com.eatzy.communication.designpattern.factory;

import lombok.*;
import java.math.BigDecimal;

/**
 * Factory Method Pattern - Concrete Product: Driver Location Notification.
 * Pushes real-time driver GPS coordinates to customer tracking their delivery.
 */
@Getter
@Setter
@NoArgsConstructor
public class DriverLocationNotification extends Notification {
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Override
    public String getDestination() {
        return "/queue/driver-location";
    }
}
