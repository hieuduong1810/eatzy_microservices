package com.eatzy.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * DTO for WebSocket chat message payloads and REST responses.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private Long orderId;
    private Long senderId;
    private Long recipientId;  // set by server from order data
    private String senderName;
    private String senderType; // "DRIVER" or "CUSTOMER"
    private String message;
    private Instant timestamp;
    private String messageType; // "TEXT", "IMAGE", "LOCATION"
}
