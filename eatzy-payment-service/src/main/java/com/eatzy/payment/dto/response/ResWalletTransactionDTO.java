package com.eatzy.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import com.eatzy.payment.domain.enums.TransactionType;
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
    private Wallet wallet;
    private Order order;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
    private String status;
    private BigDecimal balanceAfter;
    private Instant createdAt;
    private Instant transactionDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private Long id;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Wallet {
        private Long id;
        private User user;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class User {
            private Long id;
            private String name;
        }
    }
}
