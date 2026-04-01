package com.eatzy.cart.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCartDTO {

    @NotNull(message = "Restaurant parameter cannot be null")
    private Restaurant restaurant;

    private List<CartItem> cartItems;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Restaurant {
        @NotNull(message = "Restaurant ID cannot be null")
        private Long id;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        private Long id;

        @NotNull(message = "Dish parameter cannot be null")
        private Dish dish;

        @NotNull(message = "Quantity cannot be null")
        private Integer quantity;

        private List<CartItemOption> cartItemOptions;

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Dish {
            @NotNull(message = "Dish ID cannot be null")
            private Long id;
        }

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CartItemOption {
            private Long id;

            @NotNull(message = "Menu Option parameter cannot be null")
            private MenuOption menuOption;

            @Getter
            @Setter
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class MenuOption {
                @NotNull(message = "Menu Option ID cannot be null")
                private Long id;
            }
        }
    }
}
