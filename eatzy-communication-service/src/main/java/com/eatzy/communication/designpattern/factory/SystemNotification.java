package com.eatzy.communication.designpattern.factory;

import lombok.*;

/**
 * Factory Method Pattern - Concrete Product: System Notification.
 * Pushes system-wide alerts (maintenance, promotions, account updates).
 */
@Getter
@Setter
@NoArgsConstructor
public class SystemNotification extends Notification {
    private String title;
    private String severity; // "INFO", "WARNING", "ERROR"

    @Override
    public String getDestination() {
        return "/queue/system";
    }
}
