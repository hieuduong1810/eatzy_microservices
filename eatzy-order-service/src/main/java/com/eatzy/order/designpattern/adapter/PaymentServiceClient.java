package com.eatzy.order.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "eatzy-payment-service")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payment/calculate-discount")
    BigDecimal calculateVoucherDiscount(@RequestBody CalculateDiscountReq req);

    @PostMapping("/api/v1/payment/initiate")
    java.util.Map<String, Object> initiatePayment(@RequestBody ReqPaymentInitiateDTO req);

    class CalculateDiscountReq {
        private List<Long> voucherIds;
        private BigDecimal subtotal;
        private Long restaurantId;
        private Long customerId;
        private BigDecimal deliveryFee;

        public CalculateDiscountReq() {
        }

        public CalculateDiscountReq(List<Long> voucherIds, BigDecimal subtotal, Long restaurantId, Long customerId, BigDecimal deliveryFee) {
            this.voucherIds = voucherIds;
            this.subtotal = subtotal;
            this.restaurantId = restaurantId;
            this.customerId = customerId;
            this.deliveryFee = deliveryFee;
        }

        public List<Long> getVoucherIds() {
            return voucherIds;
        }

        public void setVoucherIds(List<Long> voucherIds) {
            this.voucherIds = voucherIds;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public void setRestaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public BigDecimal getDeliveryFee() {
            return deliveryFee;
        }

        public void setDeliveryFee(BigDecimal deliveryFee) {
            this.deliveryFee = deliveryFee;
        }
    }

    class ReqPaymentInitiateDTO {
        private Long orderId;
        private Long customerId;
        private BigDecimal amount;
        private String method; // VNPAY, WALLET, COD
        private String ipAddress;
        private String baseUrl;
        private Long driverId;

        public ReqPaymentInitiateDTO() {}

        public ReqPaymentInitiateDTO(Long orderId, Long customerId, BigDecimal amount, String method, String ipAddress, String baseUrl, Long driverId) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.amount = amount;
            this.method = method;
            this.ipAddress = ipAddress;
            this.baseUrl = baseUrl;
            this.driverId = driverId;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Long getDriverId() { return driverId; }
        public void setDriverId(Long driverId) { this.driverId = driverId; }
    }
}
