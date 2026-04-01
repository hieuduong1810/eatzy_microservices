package com.eatzy.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import com.eatzy.restaurant.domain.Dish;
import com.eatzy.restaurant.domain.req.ReqDishDTO;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.domain.res.ResDishDTO;
import com.eatzy.restaurant.service.DishService;
import com.eatzy.restaurant.mapper.DishMapper;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class DishController {
    private final DishService dishService;
    private final DishMapper dishMapper;

    public DishController(DishService dishService, DishMapper dishMapper) {
        this.dishService = dishService;
        this.dishMapper = dishMapper;
    }

    @PostMapping("/dishes")
    @ApiMessage("Create dish with menu options")
    public ResponseEntity<ResDishDTO> createDish(@RequestBody ReqDishDTO dto) throws IdInvalidException {
        Dish createdDish = dishService.createDishWithMenuOptions(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dishMapper.convertToResDishDTO(createdDish));
    }

    @PutMapping("/dishes")
    @ApiMessage("Update dish with menu options")
    public ResponseEntity<ResDishDTO> updateDish(@RequestBody ReqDishDTO dto) throws IdInvalidException {
        Dish updatedDish = dishService.updateDishWithMenuOptions(dto);
        return ResponseEntity.ok(dishMapper.convertToResDishDTO(updatedDish));
    }

    @GetMapping("/dishes")
    @ApiMessage("Get all dishes")
    public ResponseEntity<ResultPaginationDTO> getAllDishes(
            @Filter Specification<Dish> spec, Pageable pageable) {
        ResultPaginationDTO result = dishService.getAllDishes(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dishes/{id}")
    @ApiMessage("Get dish by id")
    public ResponseEntity<ResDishDTO> getDishById(@PathVariable("id") Long id) throws IdInvalidException {
        Dish dish = dishService.getDishById(id);
        if (dish == null) {
            throw new IdInvalidException("Dish not found with id: " + id);
        }
        return ResponseEntity.ok(dishMapper.convertToResDishDTO(dish));
    }

    @GetMapping("/dishes/restaurant/{restaurantId}")
    @ApiMessage("Get dishes by restaurant id")
    public ResponseEntity<List<ResDishDTO>> getDishesByRestaurantId(@PathVariable("restaurantId") Long restaurantId) {
        List<Dish> dishes = dishService.getDishesByRestaurantId(restaurantId);
        List<ResDishDTO> dishDTOs = dishes.stream()
                .map(dishMapper::convertToResDishDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dishDTOs);
    }

    @GetMapping("/dishes/category/{categoryId}")
    @ApiMessage("Get dishes by category id")
    public ResponseEntity<List<ResDishDTO>> getDishesByCategoryId(@PathVariable("categoryId") Long categoryId) {
        List<Dish> dishes = dishService.getDishesByCategoryId(categoryId);
        List<ResDishDTO> dishDTOs = dishes.stream()
                .map(dishMapper::convertToResDishDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dishDTOs);
    }

    @DeleteMapping("/dishes/{id}")
    @ApiMessage("Delete dish by id")
    public ResponseEntity<Void> deleteDish(@PathVariable("id") Long id) throws IdInvalidException {
        Dish dish = dishService.getDishById(id);
        if (dish == null) {
            throw new IdInvalidException("Dish not found with id: " + id);
        }
        dishService.deleteDish(id);
        return ResponseEntity.ok().body(null);
    }
}
