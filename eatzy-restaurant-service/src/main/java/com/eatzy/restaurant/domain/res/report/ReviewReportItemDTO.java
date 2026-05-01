package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.time.Instant;
import java.util.List;

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
