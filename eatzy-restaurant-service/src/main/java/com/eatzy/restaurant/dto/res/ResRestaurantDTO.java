package com.eatzy.restaurant.dto.res;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    private List<ResRestaurantTypeDTO> restaurantTypes;

    // Personalized scores
    private Double typeScore;
    private Double loyaltyScore;
    private Double distanceScore;
    private Double qualityScore;
    private Double finalScore;
}
