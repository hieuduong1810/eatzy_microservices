package com.eatzy.order.repository;

import com.eatzy.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByRestaurantId(Long restaurantId);
    List<Order> findByDriverId(Long driverId);
    List<Order> findByOrderStatus(String orderStatus);
    List<Order> findByCustomerIdAndOrderStatus(Long customerId, String orderStatus);
    List<Order> findByRestaurantIdAndOrderStatus(Long restaurantId, String orderStatus);
    List<Order> findByDriverIdAndOrderStatus(Long driverId, String orderStatus);
    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<Order> findByPaymentMethodAndPaymentStatusAndCreatedAtBefore(String paymentMethod, String paymentStatus, Instant createdAt);
    List<Order> findByOrderStatusAndCreatedAtBefore(String orderStatus, Instant createdAt);
    List<Order> findByOrderStatusAndPreparingAtBefore(String orderStatus, Instant preparingAt);
    List<Order> findByOrderStatusAndDriverIdIsNullOrderByPreparingAtAsc(String orderStatus);
    long countByOrderStatusInAndDriverIdIsNull(List<String> statuses);
    List<Order> findByOrderStatusAndDriverIdIsNotNullAndAssignedAtBefore(String orderStatus, Instant assignedAt);
    List<Order> findByRestaurantIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long restaurantId, Instant startDate, Instant endDate);
    Long countByRestaurantIdAndOrderStatusAndCreatedAtBetween(Long restaurantId, String status, Instant startDate, Instant endDate);
    Order findFirstByDriverIdAndOrderStatusIn(Long driverId, List<String> statuses);
}
