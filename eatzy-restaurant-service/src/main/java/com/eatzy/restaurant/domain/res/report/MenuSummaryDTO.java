package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuSummaryDTO {
    private Integer totalDishes;
    private Integer activeDishes;
    private Integer outOfStockDishes;
    private List<MenuAnalyticsItemDTO> topSellingDishes;
    private List<MenuAnalyticsItemDTO> lowPerformingDishes;
    private List<CategoryAnalyticsItemDTO> categoryBreakdown;
}
