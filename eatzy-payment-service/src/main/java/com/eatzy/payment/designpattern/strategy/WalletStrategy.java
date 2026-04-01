package com.eatzy.payment.designpattern.strategy;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.designpattern.adapter.OrderServiceClient;
import com.eatzy.payment.domain.Wallet;
import com.eatzy.payment.domain.WalletTransaction;
import com.eatzy.payment.dto.request.ReqPaymentInitiateDTO;
import com.eatzy.payment.service.WalletService;
import com.eatzy.payment.service.WalletTransactionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component("WALLET")
public class WalletStrategy implements PaymentStrategy {

    private final WalletService walletService;
    private final WalletTransactionService walletTransactionService;
    private final AuthServiceClient authServiceClient;
    private final OrderServiceClient orderServiceClient;

    public WalletStrategy(WalletService walletService,
                          WalletTransactionService walletTransactionService,
                          AuthServiceClient authServiceClient,
                          OrderServiceClient orderServiceClient) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.authServiceClient = authServiceClient;
        this.orderServiceClient = orderServiceClient;
    }

    private Long getAdminId() {
        try {
            Map<String, Object> response = authServiceClient.getUserByEmail("admin@gmail.com");
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("id")) {
                    return ((Number) data.get("id")).longValue();
                }
            }
        } catch (Exception e) {
            // log error
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> initiatePayment(ReqPaymentInitiateDTO req) throws Exception {
        Map<String, Object> result = new HashMap<>();

        Long customerId = req.getCustomerId();
        Long orderId = req.getOrderId();
        BigDecimal totalAmount = req.getAmount();

        if (customerId == null || orderId == null || totalAmount == null) {
            throw new IdInvalidException("Missing required parameters for WALLET payment");
        }

        Wallet customerWallet = walletService.getWalletByUserId(customerId);
        if (customerWallet == null) {
            throw new IdInvalidException("Customer wallet not found");
        }

        if (customerWallet.getBalance().compareTo(totalAmount) < 0) {
            result.put("success", false);
            result.put("message", "Insufficient wallet balance");
            return result;
        }

        WalletTransaction customerTransaction = WalletTransaction.builder()
                .wallet(customerWallet)
                .transactionType("PAYMENT")
                .amount(totalAmount.negate())
                .balanceAfter(customerWallet.getBalance().subtract(totalAmount))
                .description("Payment for order #" + orderId)
                .orderId(orderId)
                .status("SUCCESS")
                .transactionDate(Instant.now())
                .build();
        walletTransactionService.createWalletTransaction(customerTransaction);

        Long adminId = getAdminId();
        if (adminId != null) {
            Wallet adminWallet = walletService.getWalletByUserId(adminId);
            if (adminWallet != null) {
                WalletTransaction adminTransaction = WalletTransaction.builder()
                        .wallet(adminWallet)
                        .transactionType("PAYMENT_RECEIVED")
                        .amount(totalAmount)
                        .balanceAfter(adminWallet.getBalance().add(totalAmount))
                        .description("Payment received from order #" + orderId)
                        .orderId(orderId)
                        .status("SUCCESS")
                        .transactionDate(Instant.now())
                        .build();
                walletTransactionService.createWalletTransaction(adminTransaction);
            }
        }

        // Update Order status to PAID
        Map<String, Object> orderUpdate = new HashMap<>();
        orderUpdate.put("id", orderId);
        orderUpdate.put("paymentStatus", "PAID");
        try {
            orderServiceClient.updateOrder(orderUpdate);
        } catch (Exception e) {
            // If order updating fails, it might roll back transaction or we log it
            throw new Exception("Failed to update order status to PAID: " + e.getMessage());
        }

        result.put("success", true);
        result.put("message", "Payment successful");
        result.put("redirect", false);
        return result;
    }

    @Override
    public Map<String, Object> processCallback(Map<String, String> queryParams) throws Exception {
        throw new UnsupportedOperationException("Wallet payment does not support callback");
    }
}
