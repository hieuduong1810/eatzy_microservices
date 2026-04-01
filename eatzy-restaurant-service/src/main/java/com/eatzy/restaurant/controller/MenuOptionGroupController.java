package com.eatzy.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import com.eatzy.restaurant.domain.MenuOptionGroup;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.service.MenuOptionGroupService;
import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.exception.IdInvalidException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MenuOptionGroupController {
    private final MenuOptionGroupService menuOptionGroupService;

    public MenuOptionGroupController(MenuOptionGroupService menuOptionGroupService) {
        this.menuOptionGroupService = menuOptionGroupService;
    }

    @PostMapping("/menu-option-groups")
    @ApiMessage("Create new menu option group")
    public ResponseEntity<MenuOptionGroup> createMenuOptionGroup(@Valid @RequestBody MenuOptionGroup menuOptionGroup)
            throws IdInvalidException {
        MenuOptionGroup createdMenuOptionGroup = menuOptionGroupService.createMenuOptionGroup(menuOptionGroup);
        return ResponseEntity.ok(createdMenuOptionGroup);
    }

    @PutMapping("/menu-option-groups")
    @ApiMessage("Update menu option group")
    public ResponseEntity<MenuOptionGroup> updateMenuOptionGroup(@RequestBody MenuOptionGroup menuOptionGroup)
            throws IdInvalidException {
        MenuOptionGroup updatedMenuOptionGroup = menuOptionGroupService.updateMenuOptionGroup(menuOptionGroup);
        return ResponseEntity.ok(updatedMenuOptionGroup);
    }

    @GetMapping("/menu-option-groups")
    @ApiMessage("Get all menu option groups")
    public ResponseEntity<ResultPaginationDTO> getAllMenuOptionGroups(
            @Filter Specification<MenuOptionGroup> spec, Pageable pageable) {
        ResultPaginationDTO result = menuOptionGroupService.getAllMenuOptionGroups(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/menu-option-groups/{id}")
    @ApiMessage("Get menu option group by id")
    public ResponseEntity<MenuOptionGroup> getMenuOptionGroupById(@PathVariable("id") Long id)
            throws IdInvalidException {
        MenuOptionGroup menuOptionGroup = menuOptionGroupService.getMenuOptionGroupById(id);
        if (menuOptionGroup == null) {
            throw new IdInvalidException("Menu option group not found with id: " + id);
        }
        return ResponseEntity.ok(menuOptionGroup);
    }

    @GetMapping("/menu-option-groups/dish/{dishId}")
    @ApiMessage("Get menu option groups by dish id")
    public ResponseEntity<List<MenuOptionGroup>> getMenuOptionGroupsByDishId(@PathVariable("dishId") Long dishId) {
        List<MenuOptionGroup> menuOptionGroups = menuOptionGroupService.getMenuOptionGroupsByDishId(dishId);
        return ResponseEntity.ok(menuOptionGroups);
    }

    @DeleteMapping("/menu-option-groups/{id}")
    @ApiMessage("Delete menu option group by id")
    public ResponseEntity<Void> deleteMenuOptionGroup(@PathVariable("id") Long id) throws IdInvalidException {
        MenuOptionGroup menuOptionGroup = menuOptionGroupService.getMenuOptionGroupById(id);
        if (menuOptionGroup == null) {
            throw new IdInvalidException("Menu option group not found with id: " + id);
        }
        menuOptionGroupService.deleteMenuOptionGroup(id);
        return ResponseEntity.ok().body(null);
    }
}
