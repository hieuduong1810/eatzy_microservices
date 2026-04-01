package com.eatzy.restaurant.designpattern.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * Strategy for Standard restaurants: 15% commission fee.
 */
@Component("standardCommission")
public class StandardCommissionStrategy implements CommissionStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.15"); // 15%

    @Override
    public BigDecimal calculateCommission(BigDecimal totalRevenue) {
        return totalRevenue.multiply(RATE).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "STANDARD (15%)";
    }
}
