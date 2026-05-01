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
public class ResMenuOptionGroupDTO {
    private Long id;
    private String name;
    private Integer minChoices;
    private Integer maxChoices;
    private List<ResMenuOptionDTO> menuOptions;
}
