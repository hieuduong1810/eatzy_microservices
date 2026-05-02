package com.eatzy.restaurant.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderClientDTO {
    private Long id;
    private String orderStatus;
    private BigDecimal subtotal;
    private BigDecimal restaurantCommissionAmount;
    private BigDecimal restaurantNetEarning;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String cancellationReason;
    private Instant createdAt;
    private Instant deliveredAt;
    
    private Customer customer;
    private List<OrderItem> orderItems;

    @Getter
    @Setter
    public static class Customer {
        private String name;
        private String phoneNumber;
    }

    @Getter
    @Setter
    public static class OrderItem {
        private Dish dish;
        private Integer quantity;
        private BigDecimal priceAtPurchase;

        public Long getDishId() {
            return dish != null ? dish.getId() : null;
        }
    }

    @Getter
    @Setter
    public static class Dish {
        private Long id;
        private String name;
    }
}
