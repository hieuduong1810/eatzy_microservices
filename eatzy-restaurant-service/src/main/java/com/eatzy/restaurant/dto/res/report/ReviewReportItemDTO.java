package com.eatzy.restaurant.dto.res.report;

import java.time.Instant;
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
public class ReviewReportItemDTO {
    private Long id;
    private Long orderId;
    private String orderCode;
    private String customerName;
    private Integer rating;
    private String comment;
    private String reply;
    private List<String> dishNames;
    private Instant createdAt;
}
