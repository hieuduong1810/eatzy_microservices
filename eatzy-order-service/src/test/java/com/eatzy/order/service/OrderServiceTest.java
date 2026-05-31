package com.eatzy.order.service;

import com.eatzy.order.dto.response.ResDeliveryFeeDTO;
import com.eatzy.order.designpattern.template.DefaultDeliveryFeeCalculator;
import com.eatzy.order.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.common.service.MapboxService;
import com.eatzy.order.service.DynamicPricingService;
import com.eatzy.order.designpattern.adapter.SystemConfigServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private RestaurantServiceClient restaurantServiceClient;
    @Mock
    private MapboxService mapboxService;
    @Mock
    private DynamicPricingService dynamicPricingService;
    @Mock
    private DefaultDeliveryFeeCalculator deliveryFeeCalculator;
    @Mock
    private SystemConfigServiceClient systemConfigServiceClient;

    // other deps not needed for this test
    @InjectMocks
    private OrderService orderService;

    @Test
    void getDeliveryFee_successfulPath_returnsComputedFee() throws Exception {
        Long restaurantId = 10L;
        Map<String, Object> rest = Map.of("latitude", "10.0", "longitude", "20.0");
        when(restaurantServiceClient.getRestaurantById(restaurantId)).thenReturn(rest);

        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_BASE_FEE")).thenReturn(Map.of("configValue", "15000"));
        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_BASE_DISTANCE")).thenReturn(Map.of("configValue", "3"));
        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_PER_KM_FEE")).thenReturn(Map.of("configValue", "2000"));
        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_MIN_FEE")).thenReturn(Map.of("configValue", "10000"));

        BigDecimal distance = new BigDecimal("5.5");
        when(mapboxService.getDrivingDistance(new BigDecimal("10.0"), new BigDecimal("20.0"), new BigDecimal("1.0"), new BigDecimal("2.0"))).thenReturn(distance);

        when(dynamicPricingService.getSurgeMultiplier(new BigDecimal("10.0"), new BigDecimal("20.0"))).thenReturn(new BigDecimal("1.2"));

        when(deliveryFeeCalculator.calculate(distance, new BigDecimal("15000"), new BigDecimal("3"), new BigDecimal("2000"), new BigDecimal("1.2"), new BigDecimal("10000")))
                .thenReturn(new BigDecimal("19000"));

        ResDeliveryFeeDTO dto = orderService.getDeliveryFee(restaurantId, new BigDecimal("1.0"), new BigDecimal("2.0"));

        assertThat(dto).isNotNull();
        assertThat(dto.getDeliveryFee()).isEqualByComparingTo(new BigDecimal("19000.00"));
        assertThat(dto.getDistance()).isEqualByComparingTo(distance.setScale(2));
        assertThat(dto.getSurgeMultiplier()).isEqualByComparingTo(new BigDecimal("1.20"));
    }

    @Test
    void getDeliveryFee_whenMapboxFails_returnsBaseFeeAndNullDistance() throws Exception {
        Long restaurantId = 11L;
        Map<String, Object> rest = Map.of("latitude", new BigDecimal("11.0"), "longitude", new BigDecimal("21.0"));
        when(restaurantServiceClient.getRestaurantById(restaurantId)).thenReturn(rest);

        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_BASE_FEE")).thenReturn(Map.of("configValue", "12000"));
        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_BASE_DISTANCE")).thenReturn(Map.of("configValue", "2"));
        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_PER_KM_FEE")).thenReturn(Map.of("configValue", "2500"));
        when(systemConfigServiceClient.getSystemConfigurationByKey("DELIVERY_MIN_FEE")).thenReturn(Map.of("configValue", "8000"));

        when(mapboxService.getDrivingDistance(new BigDecimal("11.0"), new BigDecimal("21.0"), new BigDecimal("3.0"), new BigDecimal("4.0"))).thenReturn(null);

        ResDeliveryFeeDTO dto = orderService.getDeliveryFee(restaurantId, new BigDecimal("3.0"), new BigDecimal("4.0"));

        assertThat(dto).isNotNull();
        assertThat(dto.getDeliveryFee()).isEqualByComparingTo(new BigDecimal("12000"));
        assertThat(dto.getDistance()).isNull();
        assertThat(dto.getSurgeMultiplier()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
