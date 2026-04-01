package com.eatzy.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import com.eatzy.restaurant.domain.DishCategory;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.service.DishCategoryService;
import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.exception.IdInvalidException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DishCategoryController {
    private final DishCategoryService dishCategoryService;

    public DishCategoryController(DishCategoryService dishCategoryService) {
        this.dishCategoryService = dishCategoryService;
    }

    @PostMapping("/dish-categories")
    @ApiMessage("Create dish category")
    public ResponseEntity<DishCategory> createDishCategory(@RequestBody DishCategory dishCategory)
            throws IdInvalidException {
        DishCategory createdCategory = dishCategoryService.createDishCategory(dishCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @PutMapping("/dish-categories")
    @ApiMessage("Update dish category")
    public ResponseEntity<DishCategory> updateDishCategory(@RequestBody DishCategory dishCategory)
            throws IdInvalidException {
        DishCategory updatedCategory = dishCategoryService.updateDishCategory(dishCategory);
        return ResponseEntity.ok(updatedCategory);
    }

    @GetMapping("/dish-categories")
    @ApiMessage("Get all dish categories")
    public ResponseEntity<ResultPaginationDTO> getAllDishCategories(
            @Filter Specification<DishCategory> spec, Pageable pageable) {
        ResultPaginationDTO result = dishCategoryService.getAllDishCategories(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dish-categories/{id}")
    @ApiMessage("Get dish category by id")
    public ResponseEntity<DishCategory> getDishCategoryById(@PathVariable("id") Long id) throws IdInvalidException {
        DishCategory category = dishCategoryService.getDishCategoryById(id);
        if (category == null) {
            throw new IdInvalidException("Dish category not found with id: " + id);
        }
        return ResponseEntity.ok(category);
    }

    @GetMapping("/dish-categories/restaurant/{restaurantId}")
    @ApiMessage("Get dish categories by restaurant id")
    public ResponseEntity<List<DishCategory>> getDishCategoriesByRestaurantId(
            @PathVariable("restaurantId") Long restaurantId) {
        List<DishCategory> categories = dishCategoryService.getDishCategoriesByRestaurantId(restaurantId);
        return ResponseEntity.ok(categories);
    }

    @DeleteMapping("/dish-categories/{id}")
    @ApiMessage("Delete dish category by id")
    public ResponseEntity<Void> deleteDishCategory(@PathVariable("id") Long id) throws IdInvalidException {
        DishCategory category = dishCategoryService.getDishCategoryById(id);
        if (category == null) {
            throw new IdInvalidException("Dish category not found with id: " + id);
        }
        dishCategoryService.deleteDishCategory(id);
        return ResponseEntity.ok().body(null);
    }
}
