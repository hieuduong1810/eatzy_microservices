package com.eatzy.restaurant.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewClientDTO {
    private Long id;
    private String reviewTarget;
    private String targetName;
    private Integer rating;
    private String comment;
    private String reply;
    private Instant createdAt;
    private Order order;
    private User customer;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private Long id;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private Long id;
        private String name;
    }
}
