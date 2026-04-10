package com.eatzy.payment.designpattern.strategy;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.dto.request.ReqPaymentInitiateDTO;
import com.eatzy.payment.designpattern.adapter.SystemConfigServiceClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component("COD")
public class CodStrategy implements PaymentStrategy {

    private final AuthServiceClient authServiceClient;
    private final SystemConfigServiceClient systemConfigServiceClient;

    public CodStrategy(AuthServiceClient authServiceClient, SystemConfigServiceClient systemConfigServiceClient) {
        this.authServiceClient = authServiceClient;
        this.systemConfigServiceClient = systemConfigServiceClient;
    }

    @Override
    public Map<String, Object> initiatePayment(ReqPaymentInitiateDTO req) throws Exception {
        Map<String, Object> result = new HashMap<>();

        BigDecimal totalAmount = req.getAmount();
        Long driverId = req.getDriverId();

        if (totalAmount == null) {
            throw new IdInvalidException("Amount is required for COD payment");
        }

        BigDecimal codLimit = getSystemConfigValue("DEFAULT_COD_LIMIT", new BigDecimal("5000000"));

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

    private BigDecimal getSystemConfigValue(String key, BigDecimal defaultValue) {
        try {
            Map<String, Object> configData = systemConfigServiceClient.getSystemConfigurationByKey(key);
            if (configData != null && configData.get("configValue") != null) {
                return new BigDecimal(configData.get("configValue").toString());
            }
        } catch (Exception e) {
            // fallback to default
        }
        return defaultValue;
    }

    @Override
    public Map<String, Object> processCallback(Map<String, String> queryParams) throws Exception {
        throw new UnsupportedOperationException("COD payment does not support callback");
    }
}
