package com.eatzy.common.event;

import java.io.Serializable;
import java.math.BigDecimal;

public class OrderCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private BigDecimal totalAmount;
    private String orderStatus;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(Long orderId, Long customerId, Long restaurantId, BigDecimal totalAmount, String orderStatus) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    @Override
    public String toString() {
        return "OrderCreatedEvent{orderId=" + orderId + ", customerId=" + customerId +
                ", restaurantId=" + restaurantId + ", totalAmount=" + totalAmount +
                ", orderStatus='" + orderStatus + "'}";
    }
}
