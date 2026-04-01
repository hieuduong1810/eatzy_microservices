package com.eatzy.order.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResOrderDTO {
    private Long id;
    private Customer customer;
    private Restaurant restaurant;
    private Driver driver;
    private List<Voucher> vouchers;
    private String orderStatus;
    private String deliveryAddress;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
    private BigDecimal distance;
    private String specialInstructions;
    private BigDecimal subtotal;
    private BigDecimal restaurantCommissionAmount;
    private BigDecimal restaurantNetEarning;
    private BigDecimal deliveryFee;
    private BigDecimal driverCommissionAmount;
    private BigDecimal driverNetEarning;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String cancellationReason;
    private Instant createdAt;
    private Instant preparingAt;
    private Instant deliveredAt;
    private Long totalTripDuration;
    private String vnpayPaymentUrl;
    private List<ResOrderItemDTO> orderItems;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        private long id;
        private String name;
        private String phoneNumber;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Driver {
        private long id;
        private String name;
        private String vehicleType;
        private String vehicleDetails;
        private String averageRating;
        private String completedTrips;
        private String vehicleLicensePlate;
        private String phoneNumber;
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Restaurant {
        private long id;
        private String name;
        private String slug;
        private String address;
        private String imageUrl;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Voucher {
        private long id;
        private String code;
    }
}
