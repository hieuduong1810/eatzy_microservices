package com.eatzy.payment.designpattern.strategy;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.config.VNPayConfiguration;
import com.eatzy.payment.designpattern.adapter.OrderServiceClient;
import com.eatzy.payment.dto.request.ReqPaymentInitiateDTO;
import com.eatzy.payment.util.VNPayUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component("VNPAY")
public class VNPayStrategy implements PaymentStrategy {

    private final VNPayConfiguration vnPayConfig;
    private final OrderServiceClient orderServiceClient;

    public VNPayStrategy(VNPayConfiguration vnPayConfig, OrderServiceClient orderServiceClient) {
        this.vnPayConfig = vnPayConfig;
        this.orderServiceClient = orderServiceClient;
    }

    @Override
    public Map<String, Object> initiatePayment(ReqPaymentInitiateDTO req) throws Exception {
        Long orderId = req.getOrderId();
        BigDecimal amount = req.getAmount();
        String ipAddress = req.getIpAddress() != null ? req.getIpAddress() : "127.0.0.1";

        if (orderId == null || amount == null) {
            throw new IdInvalidException("Missing required parameters for VNPAY");
        }

        long amountInt = amount.longValue() * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amountInt));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", String.valueOf(orderId) + "_" + VNPayUtil.getRandomNumber(4));
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", ipAddress);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getUrl() + "?" + queryUrl;

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("redirect", true);
        result.put("paymentUrl", paymentUrl);
        return result;
    }

    @Override
    public Map<String, Object> processCallback(Map<String, String> queryParams) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // Remove hash params to calculate signature
        String vnp_SecureHash = queryParams.remove("vnp_SecureHash");
        queryParams.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(queryParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = queryParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        
        String signValue = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        if (!signValue.equals(vnp_SecureHash)) {
            result.put("success", false);
            result.put("message", "Invalid signature");
            return result;
        }

        String responseCode = queryParams.get("vnp_ResponseCode");
        String orderInfo = queryParams.get("vnp_TxnRef");
        
        if ("00".equals(responseCode)) {
            // Success
            String orderIdStr = orderInfo.split("_")[0];
            Long orderId = Long.parseLong(orderIdStr);

            // Update order status
            Map<String, Object> orderUpdate = new HashMap<>();
            orderUpdate.put("id", orderId);
            orderUpdate.put("paymentStatus", "PAID");
            try {
                orderServiceClient.updateOrder(orderUpdate);
            } catch (Exception e) {
                // Log the error but keep the success state for VNPay
            }

            result.put("success", true);
            result.put("message", "Payment successful");
            result.put("orderId", orderId);
        } else {
            result.put("success", false);
            result.put("message", "Payment failed with code " + responseCode);
        }

        return result;
    }
}
