package com.eatzy.interaction.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "eatzy-auth-service")
public interface AuthServiceClient {
    
    @GetMapping("/api/v1/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/profile/driver/user/{userId}")
    Map<String, Object> getDriverProfileByUserId(@PathVariable("userId") Long userId);
}
