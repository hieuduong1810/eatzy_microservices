package com.eatzy.payment.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "eatzy-order-service")
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{id}")
    Map<String, Object> getOrderById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/orders/count-usage")
    Long countByCustomerIdAndVoucherId(
            @RequestParam("customerId") Long customerId,
            @RequestParam("voucherId") Long voucherId);

    @PutMapping("/api/v1/orders")
    Map<String, Object> updateOrder(@RequestBody Map<String, Object> orderUpdate);
}
