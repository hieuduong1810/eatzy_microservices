package com.eatzy.order.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Adapter Pattern: Feign Client for communicating with Auth Service.
 */
@FeignClient(name = "eatzy-auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{userId}")
    Map<String, Object> getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/api/v1/users/email/{email}")
    Map<String, Object> getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/api/v1/driver-profiles/user/{userId}")
    Map<String, Object> getDriverProfileByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/api/v1/driver-profiles/count")
    long countDriversByStatus(@RequestParam("status") String status);

    @GetMapping("/api/v1/driver-profiles/nearby")
    List<Map<String, Object>> findNearbyDrivers(
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam("radiusKm") double radiusKm,
            @RequestParam("limit") int limit);

    @PatchMapping("/api/v1/driver-profiles/user/{userId}/status")
    void updateDriverStatus(@PathVariable("userId") Long userId, @RequestParam("status") String status);
}
