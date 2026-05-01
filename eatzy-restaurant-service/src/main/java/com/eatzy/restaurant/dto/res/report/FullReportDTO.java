package com.eatzy.restaurant.dto.res.report;

import java.math.BigDecimal;
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
public class FullReportDTO {
    private BigDecimal totalRevenue;
    private BigDecimal netRevenue;
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer cancelledOrders;
    private BigDecimal cancelRate;
    private BigDecimal averageOrderValue;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private String topPerformingDish;
    private List<RevenueReportItemDTO> revenueChart;
    private List<OrderStatusBreakdownDTO> orderStatusBreakdown;
}
