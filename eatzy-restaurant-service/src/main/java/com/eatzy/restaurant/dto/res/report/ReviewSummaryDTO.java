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
public class ReviewSummaryDTO {
    private BigDecimal averageRating;
    private Integer totalReviews;
    private RatingDistributionDTO ratingDistribution;
    private List<ReviewReportItemDTO> recentReviews;
    private BigDecimal responseRate;
    private BigDecimal averageResponseTime;
}
