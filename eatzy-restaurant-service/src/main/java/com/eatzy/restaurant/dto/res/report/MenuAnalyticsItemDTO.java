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
public class MenuAnalyticsItemDTO {
    private Long dishId;
    private String dishName;
    private String categoryName;
    private String imageUrl;
    private BigDecimal price;
    private Integer totalOrdered;
    private BigDecimal totalRevenue;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private String trend; // "up", "down", "stable"
    private BigDecimal trendPercent;
}
