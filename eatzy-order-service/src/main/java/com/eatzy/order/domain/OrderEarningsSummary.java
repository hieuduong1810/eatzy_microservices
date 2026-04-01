package com.eatzy.order.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_earnings_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEarningsSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Cross-domain: store ID only (was @ManyToOne User in monolith)
    @Column(name = "driver_id")
    private Long driverId;

    // Cross-domain: store ID only (was @ManyToOne Restaurant in monolith)
    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(precision = 10, scale = 2)
    private BigDecimal orderSubtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(precision = 5, scale = 2)
    private BigDecimal restaurantCommissionRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal restaurantCommissionAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal restaurantNetEarning;

    @Column(precision = 5, scale = 2)
    private BigDecimal driverCommissionRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal driverCommissionAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal driverNetEarning;

    @Column(precision = 10, scale = 2)
    private BigDecimal platformVoucherCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal platformTotalEarning;

    private Instant recordedAt;
}
