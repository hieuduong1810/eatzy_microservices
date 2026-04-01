package com.eatzy.cart.designpattern.decorator;

import java.math.BigDecimal;

/**
 * Concrete Component: Base Dish Price
 */
public class BaseItemPrice implements ItemPriceCalculator {

    private final BigDecimal basePrice;

    public BaseItemPrice(BigDecimal basePrice) {
        this.basePrice = basePrice != null ? basePrice : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculatePrice() {
        return this.basePrice;
    }
}
