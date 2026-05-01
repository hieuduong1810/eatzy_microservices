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
public class CategoryAnalyticsItemDTO {
    private Long categoryId;
    private String categoryName;
    private Integer totalDishes;
    private Integer totalOrdered;
    private BigDecimal totalRevenue;
    private BigDecimal percentOfTotal;
}
