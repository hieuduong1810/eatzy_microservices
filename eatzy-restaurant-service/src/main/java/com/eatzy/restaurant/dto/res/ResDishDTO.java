package com.eatzy.restaurant.dto.res;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResDishDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int availabilityQuantity;
    private String imageUrl;
    private int menuOptionGroupCount;
    private List<ResMenuOptionGroupDTO> menuOptionGroups;
}
