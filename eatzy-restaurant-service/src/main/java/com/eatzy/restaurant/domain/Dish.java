package com.eatzy.restaurant.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "dishes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private DishCategory category;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String imageUrl;
    private int availabilityQuantity;

    // TODO: orderItems, cartItems thuoc domain Order/Cart -> se giao tiep qua REST API

    @OneToMany(mappedBy = "dish")
    private List<MenuOptionGroup> menuOptionGroups;
}