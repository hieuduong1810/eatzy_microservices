package com.eatzy.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqOrderDTO {

    @NotNull(message = "Customer không được để trống")
    private Customer customer;

    @NotNull(message = "Restaurant không được để trống")
    private Restaurant restaurant;

    private Driver driver;

    private List<Voucher> vouchers;

    private String orderStatus;

    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private String deliveryAddress;

    @NotNull(message = "Latitude không được để trống")
    private Double deliveryLatitude;

    @NotNull(message = "Longitude không được để trống")
    private Double deliveryLongitude;

    private String specialInstructions;

    private BigDecimal subtotal;

    @NotNull(message = "Delivery fee không được để trống")
    private BigDecimal deliveryFee;

    private BigDecimal totalAmount;

    private String paymentMethod;
    private String paymentStatus;

    @NotNull(message = "Order items không được để trống")
    private List<OrderItem> orderItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        @NotNull(message = "Customer ID không được để trống")
        private Long id;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Restaurant {
        @NotNull(message = "Restaurant ID không được để trống")
        private Long id;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Driver {
        private Long id;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Voucher {
        private Long id;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long id;

        @NotNull(message = "Dish không được để trống")
        private Dish dish;

        @NotNull(message = "Quantity không được để trống")
        private Integer quantity;

        private List<OrderItemOption> orderItemOptions;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Dish {
            @NotNull(message = "Dish ID không được để trống")
            private Long id;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderItemOption {
            private Long id;

            @NotNull(message = "Menu Option không được để trống")
            private MenuOption menuOption;

            @Getter
            @Setter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class MenuOption {
                @NotNull(message = "Menu Option ID không được để trống")
                private Long id;
            }
        }
    }
}
