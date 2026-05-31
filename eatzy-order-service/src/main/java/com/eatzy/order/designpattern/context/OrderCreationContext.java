package com.eatzy.order.designpattern.context;

import com.eatzy.order.domain.Order;
import com.eatzy.order.domain.OrderItem;
import com.eatzy.order.dto.request.ReqOrderDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Context object used to pass state through the Order Creation Validation Chain.
 * Follows the Builder pattern.
 */
@Data
@Builder
public class OrderCreationContext {
    private ReqOrderDTO reqOrderDTO;
    private String clientIp;
    private String baseUrl;

    // Intermediate state populated by validators
    private Map<String, Object> customerData;
    private Map<String, Object> restaurantData;
    private BigDecimal deliveryFee;
    
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
    
    // The final built entity
    private Order orderEntity;
}
