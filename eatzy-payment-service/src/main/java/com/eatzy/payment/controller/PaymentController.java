package com.eatzy.payment.controller;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.dto.request.ReqPaymentInitiateDTO;
import com.eatzy.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(ReqPaymentInitiateDTO req) throws Exception {
        return ResponseEntity.ok(paymentService.initiatePayment(req));
    }

    @GetMapping("/vnpay/callback")
    public ResponseEntity<Map<String, Object>> vnpayCallback(@RequestParam Map<String, String> params)
            throws Exception {
        return ResponseEntity.ok(paymentService.handleCallback("VNPAY", params));
    }

    @PostMapping("/cod/delivery")
    public ResponseEntity<Map<String, Object>> processCODPaymentOnDelivery(
            @RequestParam("orderId") Long orderId,
            @RequestParam("driverId") Long driverId,
            @RequestParam("amount") BigDecimal amount) throws IdInvalidException {
        return ResponseEntity.ok(paymentService.processCODPaymentOnDelivery(orderId, driverId, amount));
    }

    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> processRefund(
            @RequestParam("orderId") Long orderId,
            @RequestParam("customerId") Long customerId,
            @RequestParam("amount") BigDecimal amount) throws IdInvalidException {
        return ResponseEntity.ok(paymentService.processRefund(orderId, customerId, amount));
    }

    @GetMapping("/calculate-commissions")
    public ResponseEntity<Map<String, BigDecimal>> calculateCommissions(
            @RequestParam(value = "deliveryFee", required = false) BigDecimal deliveryFee,
            @RequestParam(value = "subtotal", required = false) BigDecimal subtotal) {
        return ResponseEntity.ok(paymentService.calculateCommissions(deliveryFee, subtotal));
    }

    @PostMapping("/calculate-discount")
    public ResponseEntity<BigDecimal> calculateVoucherDiscount(@RequestBody CalculateDiscountReq req) {
        BigDecimal discount = paymentService.calculateVoucherDiscount(
                req.getVoucherIds(),
                req.getSubtotal(),
                req.getRestaurantId(),
                req.getCustomerId(),
                req.getDeliveryFee());
        return ResponseEntity.ok(discount);
    }

    public static class CalculateDiscountReq {
        private List<Long> voucherIds;
        private BigDecimal subtotal;
        private Long restaurantId;
        private Long customerId;
        private BigDecimal deliveryFee;

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
}
