package com.eatzy.order.controller;

import com.eatzy.common.dto.RestResponse;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.domain.Order;
import com.eatzy.order.dto.request.ReqDeliveryFeeDTO;
import com.eatzy.order.dto.request.ReqOrderDTO;
import com.eatzy.order.dto.response.ResDeliveryFeeDTO;
import com.eatzy.order.dto.response.ResOrderDTO;
import com.eatzy.order.service.OrderEarningsSummaryService;
import com.eatzy.order.service.OrderService;
import com.eatzy.common.util.SecurityUtils;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;
    private final OrderEarningsSummaryService earningsSummaryService;

    public OrderController(OrderService orderService,
            OrderEarningsSummaryService earningsSummaryService) {
        this.orderService = orderService;
        this.earningsSummaryService = earningsSummaryService;
    }

    // ==================== CRUD ====================

    @PostMapping("/orders")
    public ResponseEntity<ResOrderDTO> createOrder(
            @Valid @RequestBody ReqOrderDTO reqOrderDTO,
            jakarta.servlet.http.HttpServletRequest request)
            throws IdInvalidException {

        String clientIp = request.getRemoteAddr();
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

        ResOrderDTO orderDTO = orderService.createOrderFromReqDTO(reqOrderDTO, clientIp, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ResOrderDTO> getOrderById(@PathVariable("id") Long id) throws IdInvalidException {
        ResOrderDTO orderDTO = orderService.getOrderDTOById(id);
        if (orderDTO == null) {
            throw new IdInvalidException("Order not found with id: " + id);
        }
        return ResponseEntity.ok(orderDTO);
    }

    @GetMapping("/orders")
    public ResponseEntity<ResultPaginationDTO> getAllOrders(
            @Filter Specification<Order> spec, Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrdersDTO(spec, pageable));
    }

    @PutMapping("/orders")
    public ResponseEntity<ResOrderDTO> updateOrder(@RequestBody Order order) throws IdInvalidException {
        return ResponseEntity.ok(orderService.updateOrder(order));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== QUERY BY ROLE ====================

    @GetMapping("/orders/restaurant/{restaurantId}")
    public ResponseEntity<ResultPaginationDTO> getOrdersByRestaurant(
            @PathVariable("restaurantId") Long restaurantId,
            @Filter Specification<Order> spec, Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersDTOByRestaurantIdWithSpec(restaurantId, spec, pageable));
    }

    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<ResultPaginationDTO> getOrdersByCustomer(
            @PathVariable("customerId") Long customerId,
            @Filter Specification<Order> spec, Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersDTOByCustomerIdWithSpec(customerId, spec, pageable));
    }

    @GetMapping("/orders/driver/{driverId}")
    public ResponseEntity<ResultPaginationDTO> getOrdersByDriver(
            @PathVariable("driverId") Long driverId,
            @Filter Specification<Order> spec, Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersDTOByDriverIdWithSpec(driverId, spec, pageable));
    }

    @GetMapping("/orders/restaurant/{restaurantId}/status/{status}")
    public ResponseEntity<List<ResOrderDTO>> getOrdersByRestaurantAndStatus(
            @PathVariable("restaurantId") Long restaurantId, @PathVariable("status") String status) {
        return ResponseEntity.ok(orderService.getOrdersDTOByRestaurantIdAndStatus(restaurantId, status));
    }

    // ==================== ORDER LIFECYCLE (State Pattern) ====================

    @PatchMapping("/orders/{id}/accept")
    public ResponseEntity<ResOrderDTO> acceptOrderByRestaurant(@PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(orderService.acceptOrderByRestaurant(id));
    }

    @PatchMapping("/orders/{id}/reject")
    public ResponseEntity<ResOrderDTO> rejectOrderByRestaurant(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false, defaultValue = "Đơn hàng bị từ chối bởi nhà hàng") String reason)
            throws IdInvalidException {
        return ResponseEntity.ok(orderService.rejectOrderByRestaurant(id, reason));
    }

    @PatchMapping("/orders/{id}/cancel")
    public ResponseEntity<ResOrderDTO> cancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false, defaultValue = "Đơn hàng bị hủy") String reason)
            throws IdInvalidException {
        return ResponseEntity.ok(orderService.cancelOrder(id, reason));
    }

    @PatchMapping("/orders/{id}/ready")
    public ResponseEntity<ResOrderDTO> markOrderAsReady(@PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(orderService.markOrderAsReady(id));
    }

    @PatchMapping("/orders/{id}/assign-driver")
    public ResponseEntity<ResOrderDTO> assignDriver(@PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(orderService.assignDriver(id));
    }

    @PatchMapping("/orders/{id}/driver-accept")
    public ResponseEntity<ResOrderDTO> acceptOrderByDriver(
            @PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(orderService.acceptOrderByDriver(id, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/orders/{id}/driver-reject")
    public ResponseEntity<ResOrderDTO> rejectOrderByDriver(
            @PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(orderService.rejectOrderByDriver(id, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/orders/{id}/picked-up")
    public ResponseEntity<ResOrderDTO> markOrderAsPickedUp(
            @PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(orderService.markOrderAsPickedUp(id, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/orders/{id}/arrived")
    public ResponseEntity<ResOrderDTO> markOrderAsArrived(
            @PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(orderService.markOrderAsArrived(id, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/orders/{id}/delivered")
    public ResponseEntity<ResOrderDTO> markOrderAsDelivered(
            @PathVariable("id") Long id) throws IdInvalidException {
        ResOrderDTO orderDTO = orderService.markOrderAsDelivered(id, SecurityUtils.getCurrentUserId());
        // Create earnings summary after delivery
        earningsSummaryService.createEarningsSummary(id);
        return ResponseEntity.ok(orderDTO);
    }

    // ==================== DELIVERY FEE (Strategy Pattern) ====================

    @PostMapping("/orders/delivery-fee")
    public ResponseEntity<ResDeliveryFeeDTO> calculateDeliveryFee(
            @Valid @RequestBody ReqDeliveryFeeDTO reqDeliveryFeeDTO) throws IdInvalidException {
        return ResponseEntity.ok(orderService.getDeliveryFee(
                reqDeliveryFeeDTO.getRestaurantId(),
                BigDecimal.valueOf(reqDeliveryFeeDTO.getDeliveryLatitude()),
                BigDecimal.valueOf(reqDeliveryFeeDTO.getDeliveryLongitude())));
    }
}
