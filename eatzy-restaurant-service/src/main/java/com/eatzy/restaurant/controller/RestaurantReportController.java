package com.eatzy.restaurant.controller;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.restaurant.dto.res.report.*;
import com.eatzy.restaurant.service.RestaurantReportService;
import com.eatzy.restaurant.service.RestaurantService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/reports")
public class RestaurantReportController {

    private final RestaurantReportService reportService;
    private final RestaurantService restaurantService;

    public RestaurantReportController(RestaurantReportService reportService, RestaurantService restaurantService) {
        this.reportService = reportService;
        this.restaurantService = restaurantService;
    }

    private Long getMyRestaurantId() throws IdInvalidException {
        Long ownerId = SecurityUtils.getCurrentUserId();
        com.eatzy.restaurant.domain.Restaurant res = restaurantService.getRestaurantByOwnerId(ownerId);
        if (res == null) {
            throw new IdInvalidException("You don't own any restaurant");
        }
        return res.getId();
    }

    @GetMapping("/full")
    public ResponseEntity<FullReportDTO> getFullReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate)
            throws IdInvalidException {

        Long restaurantId = getMyRestaurantId();
        return ResponseEntity.ok(reportService.getFullReport(restaurantId, startDate, endDate));
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueReportItemDTO>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate)
            throws IdInvalidException {

        Long restaurantId = getMyRestaurantId();
        return ResponseEntity.ok(reportService.getRevenueReport(restaurantId, startDate, endDate));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderReportItemDTO>> getOrdersReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate)
            throws IdInvalidException {

        Long restaurantId = getMyRestaurantId();
        return ResponseEntity.ok(reportService.getOrdersReport(restaurantId, startDate, endDate));
    }

    @GetMapping("/menu")
    public ResponseEntity<MenuSummaryDTO> getMenuReport() throws IdInvalidException {

        Long restaurantId = getMyRestaurantId();
        return ResponseEntity.ok(reportService.getMenuReport(restaurantId));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ReviewSummaryDTO> getReviewReport() throws IdInvalidException {

        Long restaurantId = getMyRestaurantId();
        return ResponseEntity.ok(reportService.getReviewReport(restaurantId));
    }
}
