package com.eatzy.restaurant.domain.res;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder // ★ DESIGN PATTERN #1: Builder Pattern
public class ResRestaurantDTO {
    private Long id;
    private Long ownerId;
    private String name;
    private String slug;
    private String address;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String contactPhone;
    private String status;
    private BigDecimal commissionRate;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private String schedule;
    private String avatarUrl;
    private String coverImageUrl;
    private BigDecimal distance;
    
    // Personalized scores
    private Double typeScore;
    private Double loyaltyScore;
    private Double distanceScore;
    private Double qualityScore;
    private Double finalScore;
}
