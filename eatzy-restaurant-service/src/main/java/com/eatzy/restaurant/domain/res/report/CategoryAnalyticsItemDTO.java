package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAnalyticsItemDTO {
    private Long categoryId;
    private String categoryName;
    private Integer totalDishes;
    private Integer totalOrdered;
    private BigDecimal totalRevenue;
    private BigDecimal percentOfTotal;
}
