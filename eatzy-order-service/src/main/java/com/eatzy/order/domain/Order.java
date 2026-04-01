package com.eatzy.order.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Order entity - Builder Pattern (via Lombok @Builder).
 * Cross-domain references stored as IDs instead of JPA relations.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cross-domain: store ID only (was @ManyToOne User in monolith)
    @Column(name = "customer_id")
    private Long customerId;

    // Cross-domain: store ID only (was @ManyToOne Restaurant in monolith)
    @Column(name = "restaurant_id")
    private Long restaurantId;

    // Cross-domain: store ID only (was @ManyToOne User in monolith)
    @Column(name = "driver_id")
    private Long driverId;

    private String orderStatus;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(precision = 10, scale = 8)
    private BigDecimal deliveryLatitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal deliveryLongitude;

    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private String paymentMethod;
    private String paymentStatus;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    private Instant createdAt;
    private Instant preparingAt;
    private Instant deliveredAt;
    private Instant assignedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    // Cross-domain: store voucher IDs (was @ManyToMany Voucher in monolith)
    // Using a simple element collection to store voucher IDs
    @ElementCollection
    @CollectionTable(name = "voucher_order", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "voucher_id")
    private List<Long> voucherIds;
}
