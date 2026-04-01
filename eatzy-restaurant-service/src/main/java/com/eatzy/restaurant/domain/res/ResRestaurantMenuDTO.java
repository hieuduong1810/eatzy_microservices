package com.eatzy.restaurant.domain.res;

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
public class ResRestaurantMenuDTO {
    private Long id;
    private String name;
    private List<ResDishCategoryDTO> dishes;
}
