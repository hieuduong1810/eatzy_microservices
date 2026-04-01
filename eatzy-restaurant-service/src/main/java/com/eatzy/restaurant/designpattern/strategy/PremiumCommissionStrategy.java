package com.eatzy.restaurant.designpattern.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * Strategy for Premium restaurants: 8% commission fee (lower since they pay a monthly fee).
 */
@Component("premiumCommission")
public class PremiumCommissionStrategy implements CommissionStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.08"); // 8%

    @Override
    public BigDecimal calculateCommission(BigDecimal totalRevenue) {
        return totalRevenue.multiply(RATE).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "PREMIUM (8%)";
    }
}
