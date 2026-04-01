package com.eatzy.cart.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResCartDTO {

    private Long id;
    private Customer customer;
    private Restaurant restaurant;
    private List<CartItem> cartItems;
    private BigDecimal cartTotal; // The total of all items in cart

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Restaurant {
        private Long id;
        private String name;
        private String address;
        private String imageUrl;
        private String status;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        private Long id;
        private Dish dish;
        private Integer quantity;
        private List<CartItemOption> cartItemOptions;
        private BigDecimal totalPrice; // Total price of (dishBase + options) * quantity

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Dish {
            private Long id;
            private String name;
            private BigDecimal price;
            private String image;
        }

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CartItemOption {
            private Long id;
            private MenuOption menuOption;

            @Getter
            @Setter
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class MenuOption {
                private Long id;
                private String name;
                private BigDecimal priceAdjustment;
            }
        }
    }
}
