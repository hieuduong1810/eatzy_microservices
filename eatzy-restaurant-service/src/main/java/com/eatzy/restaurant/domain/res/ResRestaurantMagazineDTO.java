package com.eatzy.restaurant.domain.res;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResRestaurantMagazineDTO {
    private Long id;
    private String name;
    private String slug;
    private String address;
    private String description;
    private String avatarUrl;
    private Integer oneStarCount;
    private Integer twoStarCount;
    private Integer threeStarCount;
    private Integer fourStarCount;
    private Integer fiveStarCount;
    private BigDecimal averageRating;
    private BigDecimal distance;
    private List<Category> category;

    // Personalized ranking scores (optional - only set if user is logged in)
    private Double typeScore; // S_Type (40%)
    private Double loyaltyScore; // S_Quen (30%)
    private Double distanceScore; // S_Gần (20%)
    private Double qualityScore; // S_Ngon (10%)
    private Double finalScore; // Total weighted score

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Category {
        private Long id;
        private String name;
        private List<Dish> dishes;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Dish {
            private Long id;
            private String name;
            private String description;
            private BigDecimal price;
            private String imageUrl;
        }
    }
}
