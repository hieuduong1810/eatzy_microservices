package com.eatzy.order.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Adapter Pattern: Feign Client for communicating with Auth Service.
 * RestResponseDecoder in eatzy-common auto-unwraps RestResponse{statusCode, data, message},
 * so Feign methods can declare their actual return types directly.
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

    @PutMapping("/api/v1/driver-profiles/user/{userId}/status")
    void updateDriverStatus(@PathVariable("userId") Long userId, @RequestParam("status") String status);

    @PutMapping("/api/v1/driver-profiles/user/{userId}/increment-completed-trips")
    void incrementCompletedTrips(@PathVariable("userId") Long userId);

    /**
     * Validate driver IDs against SQL business rules (status = AVAILABLE, COD limit check).
     * RestResponseDecoder auto-unwraps the RestResponse wrapper from auth-service.
     */
    @PostMapping("/api/v1/driver-profiles/validate-drivers")
    List<Long> validateDriversByIds(
            @RequestBody List<Long> userIds,
            @RequestParam(value = "minCodLimit", required = false) BigDecimal minCodLimit);
}
