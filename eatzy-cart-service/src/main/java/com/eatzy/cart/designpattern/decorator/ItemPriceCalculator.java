package com.eatzy.cart.designpattern.decorator;

import java.math.BigDecimal;

/**
 * Component Interface for Decorator Pattern.
 */
public interface ItemPriceCalculator {
    BigDecimal calculatePrice();
}
