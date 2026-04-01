package com.eatzy.order.controller;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.domain.OrderEarningsSummary;
import com.eatzy.order.service.OrderEarningsSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class OrderEarningsSummaryController {

    private final OrderEarningsSummaryService earningsSummaryService;

    public OrderEarningsSummaryController(OrderEarningsSummaryService earningsSummaryService) {
        this.earningsSummaryService = earningsSummaryService;
    }

    @GetMapping("/earnings/order/{orderId}")
    public ResponseEntity<OrderEarningsSummary> getByOrder(@PathVariable Long orderId) throws IdInvalidException {
        Optional<OrderEarningsSummary> summary = earningsSummaryService.getByOrderId(orderId);
        if (summary.isEmpty()) {
            throw new IdInvalidException("Earnings summary not found for order: " + orderId);
        }
        return ResponseEntity.ok(summary.get());
    }

    @GetMapping("/earnings/driver/{driverId}/total")
    public ResponseEntity<Map<String, BigDecimal>> getDriverTotalEarnings(@PathVariable Long driverId) {
        BigDecimal total = earningsSummaryService.getDriverTotalEarnings(driverId);
        return ResponseEntity.ok(Map.of("totalEarnings", total));
    }

    @GetMapping("/earnings/restaurant/{restaurantId}/total")
    public ResponseEntity<Map<String, BigDecimal>> getRestaurantTotalEarnings(@PathVariable Long restaurantId) {
        BigDecimal total = earningsSummaryService.getRestaurantTotalEarnings(restaurantId);
        return ResponseEntity.ok(Map.of("totalEarnings", total));
    }
}
