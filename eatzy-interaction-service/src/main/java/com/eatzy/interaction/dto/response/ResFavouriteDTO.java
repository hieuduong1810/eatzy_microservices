package com.eatzy.interaction.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ResFavouriteDTO {
    private Long id;
    private User customer;
    private Restaurant restaurant;

    @Getter
    @Setter
    public static class User {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    public static class Restaurant {
        private Long id;
        private String name;
        private String slug;
        private String address;
        private String description;
        private BigDecimal averageRating;
        private String imageUrl;
        private String status;
        private List<RestaurantType> restaurantTypes;
    }

    @Getter
    @Setter
    public static class RestaurantType {
        private Long id;
        private String name;
    }
}
