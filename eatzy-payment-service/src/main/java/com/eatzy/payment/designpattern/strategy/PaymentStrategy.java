package com.eatzy.payment.designpattern.strategy;

import com.eatzy.payment.dto.request.ReqPaymentInitiateDTO;

import java.util.Map;

public interface PaymentStrategy {
    Map<String, Object> initiatePayment(ReqPaymentInitiateDTO req) throws Exception;

    Map<String, Object> processCallback(Map<String, String> queryParams) throws Exception;
}
