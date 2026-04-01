package com.eatzy.cart.designpattern.decorator;

import java.math.BigDecimal;

/**
 * Concrete Decorator: Adds Menu Option (Topping) Price
 */
public class MenuOptionDecorator extends PriceDecorator {

    private final BigDecimal optionPrice;

    public MenuOptionDecorator(ItemPriceCalculator priceCalculator, BigDecimal optionPrice) {
        super(priceCalculator);
        this.optionPrice = optionPrice != null ? optionPrice : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculatePrice() {
        // total = inner component price + option price
        return super.calculatePrice().add(this.optionPrice);
    }
}
