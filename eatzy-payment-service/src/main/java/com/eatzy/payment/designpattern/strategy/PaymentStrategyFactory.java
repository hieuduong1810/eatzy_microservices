package com.eatzy.payment.designpattern.strategy;

import com.eatzy.common.exception.IdInvalidException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentStrategyFactory {

    private final Map<String, PaymentStrategy> strategies;

    public PaymentStrategyFactory(Map<String, PaymentStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentStrategy getStrategy(String method) throws IdInvalidException {
        if (method == null || method.trim().isEmpty()) {
            throw new IdInvalidException("Payment method is required");
        }
        
        PaymentStrategy strategy = strategies.get(method.toUpperCase());
        if (strategy == null) {
            throw new IdInvalidException("Unsupported payment method: " + method);
        }
        
        return strategy;
    }
}
