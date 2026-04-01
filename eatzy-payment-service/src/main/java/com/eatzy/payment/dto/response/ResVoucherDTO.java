package com.eatzy.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResVoucherDTO {
    private Long id;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimitPerUser;
    private Instant startDate;
    private Instant endDate;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private Boolean active;
    private Integer remainingUsage;
    private String creatorType;
    
    // Flattened DTO representation of external relationships
    private List<RestaurantSummary> restaurants;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RestaurantSummary {
        private long id;
        private String name;
    }
}
