package com.eatzy.restaurant.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.eatzy.restaurant.domain.Dish;
import com.eatzy.restaurant.dto.res.ResDishDTO;
import com.eatzy.restaurant.dto.res.ResMenuOptionDTO;
import com.eatzy.restaurant.dto.res.ResMenuOptionGroupDTO;

@Component
public class DishMapper {

    public ResDishDTO convertToResDishDTO(Dish dish) {
        if (dish == null) {
            return null;
        }
        ResDishDTO dto = new ResDishDTO();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setPrice(dish.getPrice());
        dto.setAvailabilityQuantity(dish.getAvailabilityQuantity());
        dto.setImageUrl(dish.getImageUrl());

        if (dish.getMenuOptionGroups() != null) {
            dto.setMenuOptionGroupCount(dish.getMenuOptionGroups().size());
            List<ResMenuOptionGroupDTO> groupDTOs = dish.getMenuOptionGroups().stream()
                    .map(group -> {
                        ResMenuOptionGroupDTO groupDTO = new ResMenuOptionGroupDTO();
                        groupDTO.setId(group.getId());
                        groupDTO.setName(group.getGroupName());
                        groupDTO.setMinChoices(group.getMinChoices());
                        groupDTO.setMaxChoices(group.getMaxChoices());

                        if (group.getMenuOptions() != null) {
                            List<ResMenuOptionDTO> optionDTOs = group.getMenuOptions().stream()
                                    .map(option -> {
                                        ResMenuOptionDTO optionDTO = new ResMenuOptionDTO();
                                        optionDTO.setId(option.getId());
                                        optionDTO.setName(option.getName());
                                        optionDTO.setPriceAdjustment(option.getPriceAdjustment());
                                        optionDTO.setAvailable(
                                                option.getIsAvailable() != null ? option.getIsAvailable() : false);
                                        return optionDTO;
                                    })
                                    .collect(Collectors.toList());
                            groupDTO.setMenuOptions(optionDTOs);
                        }
                        return groupDTO;
                    })
                    .collect(Collectors.toList());
            dto.setMenuOptionGroups(groupDTOs);
        } else {
            dto.setMenuOptionGroupCount(0);
            dto.setMenuOptionGroups(new ArrayList<>());
        }

        return dto;
    }
}
