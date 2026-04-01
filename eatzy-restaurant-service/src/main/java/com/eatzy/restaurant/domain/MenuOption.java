package com.eatzy.restaurant.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "menu_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private MenuOptionGroup menuOptionGroup;

    private String name;

    @Column(name = "price_adjustment", precision = 10, scale = 2)
    private BigDecimal priceAdjustment;

    @Column(name = "is_available")
    private Boolean isAvailable;

    // TODO: orderItemOptions, cartItemOptions thuoc domain Order/Cart -> se giao tiep qua REST API
}
