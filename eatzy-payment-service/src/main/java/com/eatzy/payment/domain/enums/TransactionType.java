package com.eatzy.payment.domain.enums;

public enum TransactionType {
    // Earnings distribution types
    DELIVERY_EARNING,
    RESTAURANT_EARNING,
    COMMISSION_PAID,
    
    // Wallet general operations
    DEPOSIT,
    WITHDRAWAL,
    
    // Payment specific types
    PAYMENT,
    PAYMENT_RECEIVED,
    REFUND,
    COD_RECEIVED,
    VNPAY_RECEIVED,
    DEPOSIT_VNPAY
}
