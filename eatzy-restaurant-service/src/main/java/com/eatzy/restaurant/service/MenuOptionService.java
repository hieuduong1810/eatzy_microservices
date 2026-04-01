package com.eatzy.restaurant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.restaurant.domain.MenuOption;
import com.eatzy.restaurant.domain.MenuOptionGroup;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.repository.MenuOptionRepository;
import com.eatzy.restaurant.repository.MenuOptionGroupRepository;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class MenuOptionService {
    private final MenuOptionRepository menuOptionRepository;
    private final MenuOptionGroupRepository menuOptionGroupRepository;

    public MenuOptionService(MenuOptionRepository menuOptionRepository,
            MenuOptionGroupRepository menuOptionGroupRepository) {
        this.menuOptionRepository = menuOptionRepository;
        this.menuOptionGroupRepository = menuOptionGroupRepository;
    }

    public MenuOption getMenuOptionById(Long id) {
        Optional<MenuOption> menuOptionOpt = this.menuOptionRepository.findById(id);
        return menuOptionOpt.orElse(null);
    }

    public List<MenuOption> getMenuOptionsByGroupId(Long groupId) {
        return this.menuOptionRepository.findByMenuOptionGroupId(groupId);
    }

    public MenuOption createMenuOption(MenuOption menuOption) throws IdInvalidException {
        // check menu option group exists
        if (menuOption.getMenuOptionGroup() != null) {
            MenuOptionGroup group = this.menuOptionGroupRepository
                    .findById(menuOption.getMenuOptionGroup().getId()).orElse(null);
            if (group == null) {
                throw new IdInvalidException(
                        "Menu option group not found with id: " + menuOption.getMenuOptionGroup().getId());
            }
            menuOption.setMenuOptionGroup(group);
        } else {
            throw new IdInvalidException("Menu option group is required");
        }

        return menuOptionRepository.save(menuOption);
    }

    public MenuOption updateMenuOption(MenuOption menuOption) throws IdInvalidException {
        // check id
        MenuOption currentMenuOption = getMenuOptionById(menuOption.getId());
        if (currentMenuOption == null) {
            throw new IdInvalidException("Menu option not found with id: " + menuOption.getId());
        }

        if (menuOption.getName() != null) {
            currentMenuOption.setName(menuOption.getName());
        }
        if (menuOption.getPriceAdjustment() != null) {
            currentMenuOption.setPriceAdjustment(menuOption.getPriceAdjustment());
        }
        if (menuOption.getIsAvailable() != null) {
            currentMenuOption.setIsAvailable(menuOption.getIsAvailable());
        }
        if (menuOption.getMenuOptionGroup() != null) {
            MenuOptionGroup group = this.menuOptionGroupRepository
                    .findById(menuOption.getMenuOptionGroup().getId()).orElse(null);
            if (group == null) {
                throw new IdInvalidException(
                        "Menu option group not found with id: " + menuOption.getMenuOptionGroup().getId());
            }
            currentMenuOption.setMenuOptionGroup(group);
        }

        return menuOptionRepository.save(currentMenuOption);
    }

    public ResultPaginationDTO getAllMenuOptions(Specification<MenuOption> spec, Pageable pageable) {
        Page<MenuOption> page = this.menuOptionRepository.findAll(spec, pageable);
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

    public void deleteMenuOption(Long id) {
        this.menuOptionRepository.deleteById(id);
    }
}
