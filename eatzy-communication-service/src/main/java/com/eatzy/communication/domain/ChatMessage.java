package com.eatzy.communication.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Chat message entity - stores persistent chat messages between driver and customer.
 * Cross-domain references use IDs instead of JPA relations.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long senderId;

    private Long recipientId;

    @Column(columnDefinition = "TEXT")
    private String messageContent;

    private String senderType; // "DRIVER" or "CUSTOMER"
    private String messageType; // "TEXT", "IMAGE", "LOCATION"
    private Instant sentAt;
    private Boolean isRead;
}
