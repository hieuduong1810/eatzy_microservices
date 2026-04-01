package com.eatzy.interaction.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ResReviewDTO {
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
    public static class Order {
        private Long id;
    }

    @Getter
    @Setter
    public static class User {
        private Long id;
        private String name;
    }
}
