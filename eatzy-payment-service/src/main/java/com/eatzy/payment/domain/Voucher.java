package com.eatzy.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String discountType;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    private Integer usageLimitPerUser;
    private Instant startDate;
    private Instant endDate;
    private Integer totalQuantity;
    private Integer remainingQuantity;

    @Column(name = "active")
    private Boolean active = true;

    // Cross-domain mapping: store restaurant IDs 
    @ElementCollection
    @CollectionTable(name = "voucher_restaurant", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "restaurant_id")
    private List<Long> restaurantIds;
}
