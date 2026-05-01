package com.eatzy.restaurant.dto.res.report;

import java.util.List;

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
public class MenuSummaryDTO {
    private Integer totalDishes;
    private Integer activeDishes;
    private Integer outOfStockDishes;
    private List<MenuAnalyticsItemDTO> topSellingDishes;
    private List<MenuAnalyticsItemDTO> lowPerformingDishes;
    private List<CategoryAnalyticsItemDTO> categoryBreakdown;
}
