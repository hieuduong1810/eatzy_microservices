package com.eatzy.order.designpattern.template;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Template Method Pattern: Defines the skeleton of the delivery fee calculation.
 */
public abstract class DeliveryFeeTemplate {

    /**
     * The Template Method: Final to prevent overriding the algorithm structure.
     */
    public final BigDecimal calculate(BigDecimal distance, BigDecimal baseFee,
                                     BigDecimal baseDistance, BigDecimal perKmFee,
                                     BigDecimal surgeMultiplier, BigDecimal minFee) {
        
        // Step 1: Calculate the distance-based component
        BigDecimal baseAndDistanceFee = calculateBaseAndDistanceFee(distance, baseFee, baseDistance, perKmFee);
        
        // Step 2: Apply the surge multiplier (dynamic pricing)
        BigDecimal feeWithSurge = applySurge(baseAndDistanceFee, surgeMultiplier);
        
        // Step 3: Ensure the fee doesn't fall below the minimum threshold
        BigDecimal finalFee = applyMinimumFee(feeWithSurge, minFee);
        
        // Return rounded to 0 decimal places (VND)
        return finalFee.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Primitive operation: Step 1 depends on the specific logic (e.g. monolith formula vs local simulation).
     */
    protected abstract BigDecimal calculateBaseAndDistanceFee(BigDecimal distance, BigDecimal baseFee,
                                                             BigDecimal baseDistance, BigDecimal perKmFee);

    /**
     * Hook/Base operation: Step 2 defaults to multiplication but can be overridden.
     */
    protected BigDecimal applySurge(BigDecimal fee, BigDecimal surgeMultiplier) {
        BigDecimal multiplier = surgeMultiplier != null ? surgeMultiplier : BigDecimal.ONE;
        return fee.multiply(multiplier);
    }

    /**
     * Hook/Base operation: Step 3 defaults to min comparison.
     */
    protected BigDecimal applyMinimumFee(BigDecimal fee, BigDecimal minFee) {
        if (minFee != null && fee.compareTo(minFee) < 0) {
            return minFee;
        }
        return fee;
    }
}
