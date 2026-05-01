package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

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
