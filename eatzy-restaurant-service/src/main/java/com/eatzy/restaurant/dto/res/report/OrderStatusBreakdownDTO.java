package com.eatzy.restaurant.dto.res.report;

import java.math.BigDecimal;

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
public class OrderStatusBreakdownDTO {
    private String status;
    private Integer count;
    private BigDecimal percent;
}
