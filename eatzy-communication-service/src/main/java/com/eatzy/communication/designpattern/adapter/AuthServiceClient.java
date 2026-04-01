package com.eatzy.communication.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Adapter Pattern - Feign Client for interacting with Auth Service.
 */
@FeignClient(name = "eatzy-auth-service")
public interface AuthServiceClient {
    @GetMapping("/api/v1/users/{userId}")
    Map<String, Object> getUserById(@PathVariable("userId") Long userId);
}
