package com.eatzy.restaurant.designpattern.strategy.commission;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * Strategy for Free-Trial restaurants: 0% commission during trial period.
 */
@Component("freeTrialCommission")
public class FreeTrialCommissionStrategy implements CommissionStrategy {

    @Override
    public BigDecimal calculateCommission(BigDecimal totalRevenue) {
        return BigDecimal.ZERO; // Free trial - no commission
    }

    @Override
    public String getStrategyName() {
        return "FREE_TRIAL (0%)";
    }
}
