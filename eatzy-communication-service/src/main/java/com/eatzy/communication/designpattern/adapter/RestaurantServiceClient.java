package com.eatzy.communication.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Adapter Pattern - Feign Client for interacting with Restaurant Service.
 */
@FeignClient(name = "eatzy-restaurant-service")
public interface RestaurantServiceClient {
    @GetMapping("/api/v1/restaurants/{id}")
    Map<String, Object> getRestaurantById(@PathVariable("id") Long id);
}
