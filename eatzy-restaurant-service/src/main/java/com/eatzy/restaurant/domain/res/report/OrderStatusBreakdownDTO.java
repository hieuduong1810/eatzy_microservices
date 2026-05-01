package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.math.BigDecimal;

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
