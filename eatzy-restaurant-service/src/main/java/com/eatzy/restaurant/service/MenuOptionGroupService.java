package com.eatzy.restaurant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.restaurant.domain.MenuOptionGroup;
import com.eatzy.restaurant.domain.Dish;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.repository.MenuOptionGroupRepository;
import com.eatzy.restaurant.repository.DishRepository;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class MenuOptionGroupService {
    private final MenuOptionGroupRepository menuOptionGroupRepository;
    private final DishRepository dishRepository;

    public MenuOptionGroupService(MenuOptionGroupRepository menuOptionGroupRepository, DishRepository dishRepository) {
        this.menuOptionGroupRepository = menuOptionGroupRepository;
        this.dishRepository = dishRepository;
    }

    public MenuOptionGroup getMenuOptionGroupById(Long id) {
        Optional<MenuOptionGroup> menuOptionGroupOpt = this.menuOptionGroupRepository.findById(id);
        return menuOptionGroupOpt.orElse(null);
    }

    public List<MenuOptionGroup> getMenuOptionGroupsByDishId(Long dishId) {
        return this.menuOptionGroupRepository.findByDishId(dishId);
    }

    public MenuOptionGroup createMenuOptionGroup(MenuOptionGroup menuOptionGroup) throws IdInvalidException {
        // check dish exists
        if (menuOptionGroup.getDish() != null) {
            Dish dish = this.dishRepository.findById(menuOptionGroup.getDish().getId()).orElse(null);
            if (dish == null) {
                throw new IdInvalidException("Dish not found with id: " + menuOptionGroup.getDish().getId());
            }
            menuOptionGroup.setDish(dish);
        } else {
            throw new IdInvalidException("Dish is required");
        }

        return menuOptionGroupRepository.save(menuOptionGroup);
    }

    public MenuOptionGroup updateMenuOptionGroup(MenuOptionGroup menuOptionGroup) throws IdInvalidException {
        // check id
        MenuOptionGroup currentMenuOptionGroup = getMenuOptionGroupById(menuOptionGroup.getId());
        if (currentMenuOptionGroup == null) {
            throw new IdInvalidException("Menu option group not found with id: " + menuOptionGroup.getId());
        }

        if (menuOptionGroup.getGroupName() != null) {
            currentMenuOptionGroup.setGroupName(menuOptionGroup.getGroupName());
        }
        if (menuOptionGroup.getMinChoices() != null) {
            currentMenuOptionGroup.setMinChoices(menuOptionGroup.getMinChoices());
        }
        if (menuOptionGroup.getMaxChoices() != null) {
            currentMenuOptionGroup.setMaxChoices(menuOptionGroup.getMaxChoices());
        }
        if (menuOptionGroup.getDish() != null) {
            Dish dish = this.dishRepository.findById(menuOptionGroup.getDish().getId()).orElse(null);
            if (dish == null) {
                throw new IdInvalidException("Dish not found with id: " + menuOptionGroup.getDish().getId());
            }
            currentMenuOptionGroup.setDish(dish);
        }

        return menuOptionGroupRepository.save(currentMenuOptionGroup);
    }

    public ResultPaginationDTO getAllMenuOptionGroups(Specification<MenuOptionGroup> spec, Pageable pageable) {
        Page<MenuOptionGroup> page = this.menuOptionGroupRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent());
        return result;
    }

    public void deleteMenuOptionGroup(Long id) {
        this.menuOptionGroupRepository.deleteById(id);
    }
}
