package com.eatzy.communication.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Adapter Pattern - Feign Client for interacting with Order Service.
 */
@FeignClient(name = "eatzy-order-service")
public interface OrderServiceClient {
    @GetMapping("/api/v1/orders/{orderId}")
    Map<String, Object> getOrderById(@PathVariable("orderId") Long orderId);

    @GetMapping("/api/v1/orders/driver/{driverId}/active")
    Map<String, Object> getActiveOrderByDriverId(@PathVariable("driverId") Long driverId);
}
