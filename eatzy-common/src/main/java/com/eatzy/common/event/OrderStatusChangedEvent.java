package com.eatzy.common.event;

import java.io.Serializable;

public class OrderStatusChangedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String previousStatus;
    private String newStatus;
    private Long customerId;
    private Long restaurantId;
    private Long driverId;
    private String message;

    public OrderStatusChangedEvent() {
    }

    public OrderStatusChangedEvent(Long orderId, String previousStatus, String newStatus,
                                   Long customerId, Long restaurantId, Long driverId, String message) {
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.driverId = driverId;
        this.message = message;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "OrderStatusChangedEvent{orderId=" + orderId +
                ", previousStatus='" + previousStatus + "', newStatus='" + newStatus +
                "', customerId=" + customerId + ", restaurantId=" + restaurantId +
                ", driverId=" + driverId + ", message='" + message + "'}";
    }
}
