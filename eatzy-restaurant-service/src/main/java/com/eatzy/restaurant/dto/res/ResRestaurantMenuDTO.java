package com.eatzy.restaurant.dto.res;

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
