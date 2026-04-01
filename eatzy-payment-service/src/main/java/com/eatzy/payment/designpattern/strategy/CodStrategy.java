package com.eatzy.payment.designpattern.strategy;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.dto.request.ReqPaymentInitiateDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component("COD")
public class CodStrategy implements PaymentStrategy {

    private final AuthServiceClient authServiceClient;

    @Value("${foodDelivery.payment.defaultCodLimit:5000000}")
    private BigDecimal defaultCodLimit;

    public CodStrategy(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    public Map<String, Object> initiatePayment(ReqPaymentInitiateDTO req) throws Exception {
        Map<String, Object> result = new HashMap<>();

        BigDecimal totalAmount = req.getAmount();
        Long driverId = req.getDriverId();

        if (totalAmount == null) {
            throw new IdInvalidException("Amount is required for COD payment");
        }

        BigDecimal codLimit = defaultCodLimit;

        if (driverId != null) {
            try {
                Map<String, Object> profileResponse = authServiceClient.getDriverProfileByUserId(driverId);
                if (profileResponse != null && profileResponse.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) profileResponse.get("data");
                    if (data != null && data.get("codLimit") != null) {
                        codLimit = new BigDecimal(data.get("codLimit").toString());
                    }
                }
            } catch (Exception e) {
                // Keep default limit if driver profile fetch fails
            }
        }

        if (totalAmount.compareTo(codLimit) > 0) {
            result.put("success", false);
            result.put("valid", false);
            result.put("message", "Order amount exceeds COD limit");
            result.put("codLimit", codLimit);
        } else {
            result.put("success", true);
            result.put("valid", true);
            result.put("message", "COD payment initialized and validated");
            result.put("redirect", false);
        }

        return result;
    }

    @Override
    public Map<String, Object> processCallback(Map<String, String> queryParams) throws Exception {
        throw new UnsupportedOperationException("COD payment does not support callback");
    }
}
