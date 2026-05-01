package com.eatzy.restaurant.dto.res.report;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReportItemDTO {
    private Long id;
    private String orderCode; // Format: EZ{id}
    private String customerName;
    private String customerPhone;
    private Instant orderTime;
    private Instant deliveredTime;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Integer itemsCount;
    private String cancellationReason;
}
