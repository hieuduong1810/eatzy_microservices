package com.eatzy.order.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "option_name")
    private String optionName;

    @Column(name = "price_at_purchase", precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    // Cross-domain: store ID only (was @ManyToOne MenuOption in monolith)
    @Column(name = "menu_option_id")
    private Long menuOptionId;
}
