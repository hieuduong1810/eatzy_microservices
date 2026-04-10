package com.eatzy.payment.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "eatzy-auth-service")
public interface AuthServiceClient {
    
    @GetMapping("/api/v1/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/users/email/{email}")
    Map<String, Object> getUserByEmail(@PathVariable("email") String email);
    
    @GetMapping("/api/v1/driver-profiles/user/{userId}")
    Map<String, Object> getDriverProfileByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/api/v1/users/role/{roleName}")
    Map<String, Object> getUserByRoleName(@PathVariable("roleName") String roleName);
}
