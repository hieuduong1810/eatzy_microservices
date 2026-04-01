package com.eatzy.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import com.eatzy.restaurant.domain.RestaurantType;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.service.RestaurantTypeService;
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

@RestController
@RequestMapping("/api/v1")
public class RestaurantTypeController {
    private final RestaurantTypeService restaurantTypeService;

    public RestaurantTypeController(RestaurantTypeService restaurantTypeService) {
        this.restaurantTypeService = restaurantTypeService;
    }

    @PostMapping("/restaurant-types")
    @ApiMessage("Create new restaurant type")
    public ResponseEntity<RestaurantType> createRestaurantType(@Valid @RequestBody RestaurantType restaurantType)
            throws IdInvalidException {
        boolean isRestaurantTypeExist = this.restaurantTypeService.checkRestaurantTypeExists(restaurantType.getSlug());
        if (isRestaurantTypeExist) {
            throw new IdInvalidException("Restaurant type already exists with slug: " + restaurantType.getSlug());
        }
        RestaurantType createdRestaurantType = restaurantTypeService.createRestaurantType(restaurantType);
        return ResponseEntity.ok(createdRestaurantType);
    }

    @PutMapping("/restaurant-types")
    @ApiMessage("Update restaurant type")
    public ResponseEntity<RestaurantType> updateRestaurantType(@RequestBody RestaurantType restaurantType)
            throws IdInvalidException {
        RestaurantType updatedRestaurantType = restaurantTypeService.updateRestaurantType(restaurantType);
        return ResponseEntity.ok(updatedRestaurantType);
    }

    @GetMapping("/restaurant-types")
    @ApiMessage("Get all restaurant types")
    public ResponseEntity<ResultPaginationDTO> getAllRestaurantTypes(
            @Filter Specification<RestaurantType> spec, Pageable pageable) {
        ResultPaginationDTO result = restaurantTypeService.getAllRestaurantTypes(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/restaurant-types/{id}")
    @ApiMessage("Delete restaurant type by id")
    public ResponseEntity<Void> deleteRestaurantType(@PathVariable("id") Long id) throws IdInvalidException {
        RestaurantType restaurantType = restaurantTypeService.getRestaurantTypeById(id);
        if (restaurantType == null) {
            throw new IdInvalidException("Restaurant type not found with id: " + id);
        }
        restaurantTypeService.deleteRestaurantType(id);
        return ResponseEntity.ok().body(null);
    }
}
