package com.eatzy.restaurant.dto.res.report;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public class RevenueReportItemDTO {
    private LocalDate date;
    private BigDecimal foodRevenue;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netRevenue;
    private Integer totalOrders;
}
