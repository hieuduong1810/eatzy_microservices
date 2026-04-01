package com.eatzy.order.designpattern.template;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Concrete Implementation of the DeliveryFeeTemplate.
 * This matches the "Monolith Formula" specifically.
 */
@Component
public class DefaultDeliveryFeeCalculator extends DeliveryFeeTemplate {

    @Override
    protected BigDecimal calculateBaseAndDistanceFee(BigDecimal distance, BigDecimal baseFee,
                                                     BigDecimal baseDistance, BigDecimal perKmFee) {
        // Formula: F_base + D_extra * R_km
        BigDecimal extraDistance = (distance != null && baseDistance != null)
                ? distance.subtract(baseDistance).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        BigDecimal fee = (baseFee != null) ? baseFee : BigDecimal.ZERO;
        BigDecimal kmFee = (perKmFee != null) ? perKmFee : BigDecimal.ZERO;

        return fee.add(extraDistance.multiply(kmFee));
    }
}
