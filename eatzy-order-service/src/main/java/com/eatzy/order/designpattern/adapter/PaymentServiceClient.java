package com.eatzy.order.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "eatzy-payment-service")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payment/calculate-discount")
    BigDecimal calculateVoucherDiscount(@RequestBody CalculateDiscountReq req);

    @PostMapping("/api/v1/payment/initiate")
    java.util.Map<String, Object> initiatePayment(@RequestBody ReqPaymentInitiateDTO req);

    @PostMapping("/api/v1/payment/refund")
    java.util.Map<String, Object> processRefund(
            @RequestParam("orderId") Long orderId,
            @RequestParam("customerId") Long customerId,
            @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/v1/payment/cod/delivery")
    java.util.Map<String, Object> processCODPaymentOnDelivery(
            @RequestParam("orderId") Long orderId,
            @RequestParam("driverId") Long driverId,
            @RequestParam("amount") BigDecimal amount);

    /**
     * Validate which driver user IDs have wallet balance > 0.
     * Matches eatzy_backend: balance > 0 check in assignDriver business logic.
     */
    @PostMapping("/api/v1/payment/wallets/validate-balance")
    List<Long> validateDriverWalletBalances(@RequestBody List<Long> userIds);

    @GetMapping("/api/v1/vouchers/{id}")
    java.util.Map<String, Object> getVoucherById(@PathVariable("id") Long id);

    class CalculateDiscountReq {
        private List<Long> voucherIds;
        private BigDecimal subtotal;
        private Long restaurantId;
        private Long customerId;
        private BigDecimal deliveryFee;

        public CalculateDiscountReq() {
        }

        public CalculateDiscountReq(List<Long> voucherIds, BigDecimal subtotal, Long restaurantId, Long customerId,
                BigDecimal deliveryFee) {
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

        public ReqPaymentInitiateDTO() {
        }

        public ReqPaymentInitiateDTO(Long orderId, Long customerId, BigDecimal amount, String method, String ipAddress,
                String baseUrl, Long driverId) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.amount = amount;
            this.method = method;
            this.ipAddress = ipAddress;
            this.baseUrl = baseUrl;
            this.driverId = driverId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Long getDriverId() {
            return driverId;
        }

        public void setDriverId(Long driverId) {
            this.driverId = driverId;
        }
    }
}
