package com.eatzy.payment.designpattern.strategy;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.designpattern.adapter.OrderServiceClient;
import com.eatzy.payment.domain.enums.TransactionType;
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
            if (response != null && response.containsKey("id")) {
                return ((Number) response.get("id")).longValue();
            }
        } catch (Exception e) {
            System.err.println("Error fetching getAdminId from auth service: " + e.getMessage());
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
                .transactionType(TransactionType.PAYMENT)
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
                        .transactionType(TransactionType.PAYMENT_RECEIVED)
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

        result.put("success", true);
        result.put("message", "Payment successful");
        result.put("redirect", false);
        result.put("status", "PAID");
        return result;
    }

    @Override
    public Map<String, Object> processCallback(Map<String, String> queryParams) throws Exception {
        throw new UnsupportedOperationException("Wallet payment does not support callback");
    }
}
