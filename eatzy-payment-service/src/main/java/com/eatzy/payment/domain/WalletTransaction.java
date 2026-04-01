package com.eatzy.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // Cross-domain reference to Order
    @Column(name = "order_id")
    private Long orderId;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private String transactionType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String status;

    @Column(precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    private Instant createdAt;

    private Instant transactionDate;
}
