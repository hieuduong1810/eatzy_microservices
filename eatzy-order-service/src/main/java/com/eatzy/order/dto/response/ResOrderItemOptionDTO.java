package com.eatzy.order.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResOrderItemOptionDTO {
    private Long id;
    private String optionName;
    private BigDecimal priceAtPurchase;
    private MenuOption menuOption;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class MenuOption {
        private long id;
        private String name;
        private BigDecimal priceAdjustment;
    }
}
