package com.eatzy.order.repository;

import com.eatzy.order.domain.OrderItemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemOptionRepository extends JpaRepository<OrderItemOption, Long>, JpaSpecificationExecutor<OrderItemOption> {
    List<OrderItemOption> findByOrderItemId(Long orderItemId);
}
