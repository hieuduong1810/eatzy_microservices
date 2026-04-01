package com.eatzy.communication.designpattern.factory;

import lombok.*;
import java.time.Instant;

/**
 * Factory Method Pattern - Abstract Product.
 * Base class for all notification types pushed via WebSocket.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Notification {
    private String type;
    private String recipientEmail;
    private String message;
    private Object data;
    private Instant timestamp;

    /**
     * Each notification type defines its own WebSocket destination.
     * Factory Method: subclasses return their specific destination.
     */
    public abstract String getDestination();
}
