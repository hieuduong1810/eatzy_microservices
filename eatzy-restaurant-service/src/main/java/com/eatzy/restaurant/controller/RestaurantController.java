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

import com.eatzy.restaurant.mapper.RestaurantMapper;

@RestController
@RequestMapping("/api/v1")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantMapper restaurantMapper;

    public RestaurantController(RestaurantService restaurantService, RestaurantMapper restaurantMapper) {
        this.restaurantService = restaurantService;
        this.restaurantMapper = restaurantMapper;
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
        return ResponseEntity.ok(restaurantMapper.convertToDTO(restaurant));
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
     * ★ OBSERVER PATTERN: Duyet nha hang -> phat event -> Listener gui thong bao tu
     * dong.
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
     * VD: /api/v1/restaurants/1/distance?latitude=10.762&longitude=106.660
     */
    @GetMapping("/restaurants/{id}/distance")
    @ApiMessage("Calculate distance to restaurant (Adapter Pattern)")
    public ResponseEntity<java.util.Map<String, Object>> calculateDistance(
            @PathVariable("id") Long id,
            @RequestParam("latitude") BigDecimal lat,
            @RequestParam("longitude") BigDecimal lng)
            throws IdInvalidException {
        BigDecimal distance = restaurantService.calculateDistanceToRestaurant(id, lat, lng);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("restaurantId", id);
        response.put("userLocation", lat + "," + lng);
        response.put("distanceKm", distance);
        return ResponseEntity.ok(response);
    }

    // ==================== CUSTOMER APIs ====================

    @GetMapping("/restaurants/nearby")
    @ApiMessage("Get nearby restaurants with scores")
    public ResponseEntity<ResultPaginationDTO> getNearbyRestaurants(
            @RequestParam(value = "latitude", required = false) BigDecimal lat,
            @RequestParam(value = "longitude", required = false) BigDecimal lng,
            @RequestParam(value = "keyword", required = false) String keyword,
            @Filter Specification<Restaurant> spec, Pageable pageable) {
        return ResponseEntity.ok(restaurantService.getNearbyRestaurants(lat, lng, keyword, spec, pageable));
    }

    @GetMapping("/restaurants/{id}/menu")
    @ApiMessage("Get restaurant menu")
    public ResponseEntity<com.eatzy.restaurant.domain.res.ResRestaurantMenuDTO> getRestaurantMenu(
            @PathVariable("id") Long id) throws IdInvalidException {
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        if (restaurant == null) {
            throw new IdInvalidException("Restaurant not found with id: " + id);
        }
        return ResponseEntity.ok(restaurantMapper.convertToMenuDTO(restaurant));
    }

    @GetMapping("/restaurants/slug/{slug}")
    @ApiMessage("Get restaurant by slug")
    public ResponseEntity<ResRestaurantDTO> getRestaurantBySlug(@PathVariable("slug") String slug)
            throws IdInvalidException {
        return ResponseEntity.ok(restaurantService.getRestaurantDTOBySlug(slug));
    }

    // ==================== OWNER APIs ====================
    // Note: In real app, consider passing user ID via a resolved parameter instead
    // of direct static call, but following plan.

    @GetMapping("/restaurants/my-restaurant")
    @ApiMessage("Get my restaurant")
    public ResponseEntity<ResRestaurantDTO> getMyRestaurant() throws IdInvalidException {
        Long ownerId = com.eatzy.common.util.SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(restaurantService.getCurrentOwnerRestaurant(ownerId));
    }

    @PutMapping("/restaurants/my-restaurant")
    @ApiMessage("Update my restaurant")
    public ResponseEntity<ResRestaurantDTO> updateMyRestaurant(@RequestBody Restaurant restaurantData)
            throws IdInvalidException {
        Long ownerId = com.eatzy.common.util.SecurityUtils.getCurrentUserId();
        Restaurant myRestaurant = restaurantService.getRestaurantByOwnerId(ownerId);
        if (myRestaurant == null) {
            throw new IdInvalidException("Owner does not have a restaurant");
        }
        restaurantData.setId(myRestaurant.getId());
        return ResponseEntity.ok(restaurantService.updateRestaurant(restaurantData));
    }

    @GetMapping("/restaurants/my-restaurant/menu")
    @ApiMessage("Get my restaurant menu")
    public ResponseEntity<com.eatzy.restaurant.domain.res.ResRestaurantMenuDTO> getMyRestaurantMenu()
            throws IdInvalidException {
        Long ownerId = com.eatzy.common.util.SecurityUtils.getCurrentUserId();
        Restaurant myRestaurant = restaurantService.getRestaurantByOwnerId(ownerId);
        if (myRestaurant == null) {
            throw new IdInvalidException("Owner does not have a restaurant");
        }
        return ResponseEntity.ok(restaurantMapper.convertToMenuDTO(myRestaurant));
    }

    @GetMapping("/restaurants/my-restaurant/status")
    @ApiMessage("Get my restaurant status")
    public ResponseEntity<java.util.Map<String, String>> getMyRestaurantStatus() throws IdInvalidException {
        Long ownerId = com.eatzy.common.util.SecurityUtils.getCurrentUserId();
        Restaurant myRestaurant = restaurantService.getRestaurantByOwnerId(ownerId);
        if (myRestaurant == null) {
            throw new IdInvalidException("Owner does not have a restaurant");
        }
        return ResponseEntity.ok(
                java.util.Map.of("status", myRestaurant.getStatus() != null ? myRestaurant.getStatus() : "UNKNOWN"));
    }

    @PostMapping("/restaurants/open")
    @ApiMessage("Open my restaurant")
    public ResponseEntity<Void> openMyRestaurant() throws IdInvalidException {
        Long ownerId = com.eatzy.common.util.SecurityUtils.getCurrentUserId();
        restaurantService.openCurrentRestaurant(ownerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restaurants/close")
    @ApiMessage("Close my restaurant")
    public ResponseEntity<Void> closeMyRestaurant() throws IdInvalidException {
        Long ownerId = com.eatzy.common.util.SecurityUtils.getCurrentUserId();
        restaurantService.closeCurrentRestaurant(ownerId);
        return ResponseEntity.ok().build();
    }
}
