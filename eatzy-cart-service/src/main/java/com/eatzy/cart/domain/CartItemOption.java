package com.eatzy.cart.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_item_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    // Decoupled from MenuOption entity in Restaurant Service
    @Column(name = "option_id", nullable = false)
    private Long menuOptionId;
}
