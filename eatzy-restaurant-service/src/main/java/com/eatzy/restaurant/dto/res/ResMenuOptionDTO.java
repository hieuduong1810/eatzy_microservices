package com.eatzy.restaurant.dto.res;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResMenuOptionDTO {
    private Long id;
    private String name;
    private BigDecimal priceAdjustment;
    private boolean isAvailable;
}
