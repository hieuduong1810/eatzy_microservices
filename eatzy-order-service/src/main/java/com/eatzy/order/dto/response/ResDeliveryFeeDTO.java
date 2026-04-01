package com.eatzy.order.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResDeliveryFeeDTO {
    private BigDecimal deliveryFee;
    private BigDecimal distance;
    private BigDecimal surgeMultiplier;
    private BigDecimal baseFee;
    private BigDecimal baseDistance;
    private BigDecimal perKmFee;
}
