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
public class ResDishCategoryDTO {
    private Long id;
    private String name;
    private List<ResDishDTO> dishes;
}
