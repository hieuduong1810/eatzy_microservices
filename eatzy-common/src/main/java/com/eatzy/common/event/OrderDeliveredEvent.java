package com.eatzy.common.event;

import java.io.Serializable;
import java.math.BigDecimal;

public class OrderDeliveredEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long customerId;
    private Long driverId;
    private Long restaurantId;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal driverNetEarning;
    private BigDecimal restaurantNetEarning;
    private java.util.List<Long> voucherIds;

    public OrderDeliveredEvent() {
    }

    public OrderDeliveredEvent(Long orderId, Long customerId, Long driverId, Long restaurantId,
                               BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal discountAmount,
                               BigDecimal totalAmount, BigDecimal driverNetEarning, BigDecimal restaurantNetEarning,
                               java.util.List<Long> voucherIds) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.driverId = driverId;
        this.restaurantId = restaurantId;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.driverNetEarning = driverNetEarning;
        this.restaurantNetEarning = restaurantNetEarning;
        this.voucherIds = voucherIds;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDriverNetEarning() { return driverNetEarning; }
    public void setDriverNetEarning(BigDecimal driverNetEarning) { this.driverNetEarning = driverNetEarning; }
    public BigDecimal getRestaurantNetEarning() { return restaurantNetEarning; }
    public void setRestaurantNetEarning(BigDecimal restaurantNetEarning) { this.restaurantNetEarning = restaurantNetEarning; }
    public java.util.List<Long> getVoucherIds() { return voucherIds; }
    public void setVoucherIds(java.util.List<Long> voucherIds) { this.voucherIds = voucherIds; }

    @Override
    public String toString() {
        return "OrderDeliveredEvent{orderId=" + orderId + ", customerId=" + customerId +
                ", driverId=" + driverId + ", restaurantId=" + restaurantId +
                ", totalAmount=" + totalAmount + "}";
    }
}
