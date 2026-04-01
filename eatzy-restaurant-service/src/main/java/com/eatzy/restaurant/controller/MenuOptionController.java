package com.eatzy.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import com.eatzy.restaurant.domain.MenuOption;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.service.MenuOptionService;
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
public class MenuOptionController {
    private final MenuOptionService menuOptionService;

    public MenuOptionController(MenuOptionService menuOptionService) {
        this.menuOptionService = menuOptionService;
    }

    @PostMapping("/menu-options")
    @ApiMessage("Create new menu option")
    public ResponseEntity<MenuOption> createMenuOption(@Valid @RequestBody MenuOption menuOption)
            throws IdInvalidException {
        MenuOption createdMenuOption = menuOptionService.createMenuOption(menuOption);
        return ResponseEntity.ok(createdMenuOption);
    }

    @PutMapping("/menu-options")
    @ApiMessage("Update menu option")
    public ResponseEntity<MenuOption> updateMenuOption(@RequestBody MenuOption menuOption)
            throws IdInvalidException {
        MenuOption updatedMenuOption = menuOptionService.updateMenuOption(menuOption);
        return ResponseEntity.ok(updatedMenuOption);
    }

    @GetMapping("/menu-options")
    @ApiMessage("Get all menu options")
    public ResponseEntity<ResultPaginationDTO> getAllMenuOptions(
            @Filter Specification<MenuOption> spec, Pageable pageable) {
        ResultPaginationDTO result = menuOptionService.getAllMenuOptions(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/menu-options/{id}")
    @ApiMessage("Get menu option by id")
    public ResponseEntity<MenuOption> getMenuOptionById(@PathVariable("id") Long id) throws IdInvalidException {
        MenuOption menuOption = menuOptionService.getMenuOptionById(id);
        if (menuOption == null) {
            throw new IdInvalidException("Menu option not found with id: " + id);
        }
        return ResponseEntity.ok(menuOption);
    }

    @GetMapping("/menu-options/group/{groupId}")
    @ApiMessage("Get menu options by group id")
    public ResponseEntity<List<MenuOption>> getMenuOptionsByGroupId(@PathVariable("groupId") Long groupId) {
        List<MenuOption> menuOptions = menuOptionService.getMenuOptionsByGroupId(groupId);
        return ResponseEntity.ok(menuOptions);
    }

    @DeleteMapping("/menu-options/{id}")
    @ApiMessage("Delete menu option by id")
    public ResponseEntity<Void> deleteMenuOption(@PathVariable("id") Long id) throws IdInvalidException {
        MenuOption menuOption = menuOptionService.getMenuOptionById(id);
        if (menuOption == null) {
            throw new IdInvalidException("Menu option not found with id: " + id);
        }
        menuOptionService.deleteMenuOption(id);
        return ResponseEntity.ok().body(null);
    }
}
