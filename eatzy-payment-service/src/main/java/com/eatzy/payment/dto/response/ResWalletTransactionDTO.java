package com.eatzy.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResWalletTransactionDTO {
    private Long id;
    private Long walletId;
    private Long orderId;
    private BigDecimal amount;
    private String transactionType;
    private String description;
    private String status;
    private BigDecimal balanceAfter;
    private Instant createdAt;
    private Instant transactionDate;
}
