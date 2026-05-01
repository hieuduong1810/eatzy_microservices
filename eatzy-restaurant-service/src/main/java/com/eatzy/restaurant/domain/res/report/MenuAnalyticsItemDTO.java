package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.math.BigDecimal;

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
    private String trend;          // "up", "down", "stable"
    private BigDecimal trendPercent;
}
