package com.eatzy.order.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Adapter Pattern: Feign Client for communicating with Restaurant Service.
 */
@FeignClient(name = "eatzy-restaurant-service")
public interface RestaurantServiceClient {

    @GetMapping("/api/v1/restaurants/{restaurantId}")
    Map<String, Object> getRestaurantById(@PathVariable("restaurantId") Long restaurantId);

    @GetMapping("/api/v1/dishes/{dishId}")
    Map<String, Object> getDishById(@PathVariable("dishId") Long dishId);

    @GetMapping("/api/v1/menu-options/{menuOptionId}")
    Map<String, Object> getMenuOptionById(@PathVariable("menuOptionId") Long menuOptionId);

    @GetMapping("/api/v1/distance")
    DistanceRes getDrivingDistance(
            @RequestParam("fromLat") BigDecimal fromLat,
            @RequestParam("fromLng") BigDecimal fromLng,
            @RequestParam("toLat") BigDecimal toLat,
            @RequestParam("toLng") BigDecimal toLng);

    class DistanceRes {
        private BigDecimal distance;
        public BigDecimal getDistance() { return distance; }
        public void setDistance(BigDecimal distance) { this.distance = distance; }
    }
}
