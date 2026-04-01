package com.eatzy.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload for Driver Location Updates via WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationUpdate {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Instant timestamp;
}
