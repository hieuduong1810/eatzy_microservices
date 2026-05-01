package com.eatzy.restaurant.client;

import com.eatzy.restaurant.client.dto.OrderClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

@FeignClient(name = "eatzy-order-service")
public interface OrderServiceClient {

        @GetMapping("/api/v1/orders/restaurant/{restaurantId}/date-range")
        List<OrderClientDTO> getOrdersByRestaurantAndDateRange(
                        @PathVariable("restaurantId") Long restaurantId,
                        @RequestParam(value = "startDate", required = false) Instant startDate,
                        @RequestParam(value = "endDate", required = false) Instant endDate);

        @GetMapping("/api/v1/orders/restaurant/{restaurantId}")
        com.eatzy.common.dto.ResultPaginationDTO getOrdersByRestaurant(
                        @PathVariable("restaurantId") Long restaurantId,
                        @RequestParam("page") int page,
                        @RequestParam("size") int size);
}
