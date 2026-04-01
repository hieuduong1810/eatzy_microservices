package com.eatzy.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cross-domain reference to User ID
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(precision = 15, scale = 2)
    private BigDecimal balance;
}
