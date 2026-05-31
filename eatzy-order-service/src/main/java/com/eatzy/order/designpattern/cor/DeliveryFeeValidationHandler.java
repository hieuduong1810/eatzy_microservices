package com.eatzy.order.designpattern.cor;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.adapter.SystemConfigServiceClient;
import com.eatzy.order.designpattern.context.OrderCreationContext;
import com.eatzy.order.dto.request.ReqOrderDTO;
import com.eatzy.order.designpattern.template.DefaultDeliveryFeeCalculator;
import com.eatzy.common.service.MapboxService;
import com.eatzy.order.service.DynamicPricingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DeliveryFeeValidationHandler extends OrderValidationHandler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryFeeValidationHandler.class);

    private final SystemConfigServiceClient systemConfigServiceClient;
    private final MapboxService mapboxService;
    private final DynamicPricingService dynamicPricingService;
    private final DefaultDeliveryFeeCalculator deliveryFeeCalculator;

    public DeliveryFeeValidationHandler(SystemConfigServiceClient systemConfigServiceClient,
                                        MapboxService mapboxService,
                                        DynamicPricingService dynamicPricingService,
                                        DefaultDeliveryFeeCalculator deliveryFeeCalculator) {
        this.systemConfigServiceClient = systemConfigServiceClient;
        this.mapboxService = mapboxService;
        this.dynamicPricingService = dynamicPricingService;
        this.deliveryFeeCalculator = deliveryFeeCalculator;
    }

    @Override
    public void handle(OrderCreationContext context) throws IdInvalidException {
        ReqOrderDTO reqOrderDTO = context.getReqOrderDTO();
        Map<String, Object> restData = context.getRestaurantData();

        // 4. Calculate delivery fee using real driving distance & dynamic pricing
        BigDecimal baseFee = getSystemConfigValue("DELIVERY_BASE_FEE");
        BigDecimal baseDistance = getSystemConfigValue("DELIVERY_BASE_DISTANCE");
        BigDecimal perKmFee = getSystemConfigValue("DELIVERY_PER_KM_FEE");
        BigDecimal minFee = getSystemConfigValue("DELIVERY_MIN_FEE");

        BigDecimal restLat = getBigDecimalValue(restData, "latitude");
        BigDecimal restLng = getBigDecimalValue(restData, "longitude");
        BigDecimal deliveryFee = baseFee;

        BigDecimal deliveryLatitude = reqOrderDTO.getDeliveryLatitude() != null ? BigDecimal.valueOf(reqOrderDTO.getDeliveryLatitude()) : null;
        BigDecimal deliveryLongitude = reqOrderDTO.getDeliveryLongitude() != null ? BigDecimal.valueOf(reqOrderDTO.getDeliveryLongitude()) : null;

        if (restLat != null && restLng != null && deliveryLatitude != null && deliveryLongitude != null) {
            BigDecimal distance = mapboxService.getDrivingDistance(restLat, restLng, deliveryLatitude, deliveryLongitude);

            if (distance != null) {
                BigDecimal surgeMultiplier = dynamicPricingService.getSurgeMultiplier(restLat, restLng);
                deliveryFee = deliveryFeeCalculator.calculate(distance, baseFee, baseDistance, perKmFee, surgeMultiplier, minFee);
            }
        }

        // Validate delivery fee
        if (reqOrderDTO.getDeliveryFee() != null) {
            BigDecimal clientFee = reqOrderDTO.getDeliveryFee().setScale(0, RoundingMode.HALF_UP);
            BigDecimal serverFee = deliveryFee.setScale(0, RoundingMode.HALF_UP);
            if (clientFee.compareTo(serverFee) != 0) {
                throw new IdInvalidException("Phí giao hàng đã thay đổi. Vui lòng tải lại trang. (Giá cũ: " + clientFee
                        + " VND, Giá mới: " + serverFee + " VND)");
            }
        }
        
        context.setDeliveryFee(deliveryFee);

        super.handle(context);
    }

    private BigDecimal getSystemConfigValue(String key) {
        try {
            Map<String, Object> configData = systemConfigServiceClient.getSystemConfigurationByKey(key);
            if (configData != null && configData.get("configValue") != null) {
                return new BigDecimal(configData.get("configValue").toString());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch system config for key: {}. Reason: {}", key, e.getMessage());
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }
}
