package com.eatzy.interaction.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "eatzy-order-service")
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{id}")
    Map<String, Object> getOrderById(@PathVariable("id") Long id);
}
