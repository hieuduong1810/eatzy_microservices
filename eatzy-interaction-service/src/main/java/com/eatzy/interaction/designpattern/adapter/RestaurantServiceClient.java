package com.eatzy.interaction.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "eatzy-restaurant-service")
public interface RestaurantServiceClient {

    @GetMapping("/api/v1/restaurants/{id}")
    Map<String, Object> getRestaurantById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/restaurants/name/{name}")
    Map<String, Object> getRestaurantByName(@PathVariable("name") String name);

    @GetMapping("/api/v1/restaurants/my-restaurant")
    Map<String, Object> getMyRestaurant();
}
