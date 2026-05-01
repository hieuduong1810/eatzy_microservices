package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

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
    private BigDecimal responseRate;        // % đánh giá đã trả lời
    private BigDecimal averageResponseTime; // Thời gian trả lời trung bình (phút)
}
