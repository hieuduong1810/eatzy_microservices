package com.eatzy.cart.designpattern.decorator;

import java.math.BigDecimal;

/**
 * Base Abstract Decorator
 */
public abstract class PriceDecorator implements ItemPriceCalculator {

    protected final ItemPriceCalculator priceCalculator;

    public PriceDecorator(ItemPriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public BigDecimal calculatePrice() {
        return this.priceCalculator.calculatePrice();
    }
}
