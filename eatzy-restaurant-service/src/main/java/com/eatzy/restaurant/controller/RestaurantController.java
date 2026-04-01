package com.eatzy.restaurant.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.domain.res.ResRestaurantDTO;
import com.eatzy.restaurant.service.RestaurantService;
import com.turkraft.springfilter.boot.Filter;

@RestController
@RequestMapping("/api/v1")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    // ==================== CRUD APIs ====================

    @PostMapping("/restaurants")
    @ApiMessage("Create restaurant")
    public ResponseEntity<ResRestaurantDTO> createRestaurant(@RequestBody Restaurant restaurant)
            throws IdInvalidException {
        ResRestaurantDTO created = restaurantService.createRestaurant(restaurant);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/restaurants")
    @ApiMessage("Update restaurant")
    public ResponseEntity<ResRestaurantDTO> updateRestaurant(@RequestBody Restaurant restaurant)
            throws IdInvalidException {
        ResRestaurantDTO updated = restaurantService.updateRestaurant(restaurant);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/restaurants")
    @ApiMessage("Get all restaurants")
    public ResponseEntity<ResultPaginationDTO> getAllRestaurants(
            @Filter Specification<Restaurant> spec, Pageable pageable) {
        ResultPaginationDTO result = restaurantService.getAllRestaurants(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/restaurants/{id}")
    @ApiMessage("Get restaurant by id")
    public ResponseEntity<ResRestaurantDTO> getRestaurantById(@PathVariable("id") Long id)
            throws IdInvalidException {
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        if (restaurant == null) {
            throw new IdInvalidException("Restaurant not found with id: " + id);
        }
        return ResponseEntity.ok(ResRestaurantDTO.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .slug(restaurant.getSlug())
                .address(restaurant.getAddress())
                .description(restaurant.getDescription())
                .status(restaurant.getStatus())
                .build());
    }

    @DeleteMapping("/restaurants/{id}")
    @ApiMessage("Delete restaurant by id")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable("id") Long id) throws IdInvalidException {
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        if (restaurant == null) {
            throw new IdInvalidException("Restaurant not found with id: " + id);
        }
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.ok().body(null);
    }

    // ==================== DESIGN PATTERN APIs ====================

    /**
     * ★ OBSERVER PATTERN: Duyet nha hang -> phat event -> Listener gui thong bao tu dong.
     */
    @PostMapping("/restaurants/{id}/approve")
    @ApiMessage("Approve restaurant (Observer Pattern)")
    public ResponseEntity<ResRestaurantDTO> approveRestaurant(@PathVariable("id") Long id)
            throws IdInvalidException {
        ResRestaurantDTO approved = restaurantService.approveRestaurant(id);
        return ResponseEntity.ok(approved);
    }

    /**
     * ★ STRATEGY PATTERN: Tinh hoa hong dua tren hang muc nha hang.
     * VD: /api/v1/restaurants/1/commission?revenue=1000000&tier=standardCommission
     */
    @GetMapping("/restaurants/{id}/commission")
    @ApiMessage("Calculate commission (Strategy Pattern)")
    public ResponseEntity<java.util.Map<String, Object>> calculateCommission(
            @PathVariable("id") Long id,
            @RequestParam("revenue") BigDecimal revenue,
            @RequestParam(value = "tier", defaultValue = "standardCommission") String tier)
            throws IdInvalidException {
        BigDecimal commission = restaurantService.calculateCommission(id, revenue, tier);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("restaurantId", id);
        response.put("revenue", revenue);
        response.put("tier", tier);
        response.put("commission", commission);
        return ResponseEntity.ok(response);
    }

    /**
     * ★ ADAPTER PATTERN: Tinh khoang cach tu nguoi dung den nha hang.
     * VD: /api/v1/restaurants/1/distance?lat=10.762&lng=106.660
     */
    @GetMapping("/restaurants/{id}/distance")
    @ApiMessage("Calculate distance to restaurant (Adapter Pattern)")
    public ResponseEntity<java.util.Map<String, Object>> calculateDistance(
            @PathVariable("id") Long id,
            @RequestParam("lat") BigDecimal lat,
            @RequestParam("lng") BigDecimal lng)
            throws IdInvalidException {
        BigDecimal distance = restaurantService.calculateDistanceToRestaurant(id, lat, lng);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("restaurantId", id);
        response.put("userLocation", lat + "," + lng);
        response.put("distanceKm", distance);
        return ResponseEntity.ok(response);
    }
}
