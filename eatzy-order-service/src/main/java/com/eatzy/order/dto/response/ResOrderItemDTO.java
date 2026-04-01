package com.eatzy.order.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResOrderItemDTO {
    private Long id;
    private Dish dish;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private List<ResOrderItemOptionDTO> orderItemOptions;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class Dish {
        private long id;
        private String name;
        private BigDecimal price;
    }
}
