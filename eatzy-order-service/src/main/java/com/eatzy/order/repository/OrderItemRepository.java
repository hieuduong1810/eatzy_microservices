package com.eatzy.order.repository;

import com.eatzy.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, JpaSpecificationExecutor<OrderItem> {
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByDishId(Long dishId);
}
