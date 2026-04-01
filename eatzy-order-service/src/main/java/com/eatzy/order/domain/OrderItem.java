package com.eatzy.order.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Cross-domain: store ID only (was @ManyToOne Dish in monolith)
    @Column(name = "dish_id")
    private Long dishId;

    private Integer quantity;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL)
    private List<OrderItemOption> orderItemOptions;

    @Column(precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;
}
