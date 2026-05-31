package com.eatzy.order.designpattern.cor;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.order.designpattern.context.OrderCreationContext;
import com.eatzy.order.domain.OrderItem;
import com.eatzy.order.domain.OrderItemOption;
import com.eatzy.order.dto.request.ReqOrderDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OrderItemsValidationHandler extends OrderValidationHandler {

    private final RestaurantServiceClient restaurantServiceClient;

    public OrderItemsValidationHandler(RestaurantServiceClient restaurantServiceClient) {
        this.restaurantServiceClient = restaurantServiceClient;
    }

    @Override
    public void handle(OrderCreationContext context) throws IdInvalidException {
        ReqOrderDTO reqOrderDTO = context.getReqOrderDTO();
        
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        if (reqOrderDTO.getOrderItems() != null && !reqOrderDTO.getOrderItems().isEmpty()) {
            for (ReqOrderDTO.OrderItem reqItem : reqOrderDTO.getOrderItems()) {
                if (reqItem.getDish() == null || reqItem.getDish().getId() == null) {
                    throw new IdInvalidException("Dish is required for order item");
                }
                
                // Validate dish via REST
                Map<String, Object> dishData = restaurantServiceClient.getDishById(reqItem.getDish().getId());
                if (dishData == null) {
                    throw new IdInvalidException("Dish not found with id: " + reqItem.getDish().getId());
                }
                if (reqItem.getQuantity() == null || reqItem.getQuantity() <= 0) {
                    throw new IdInvalidException("Quantity must be greater than 0");
                }

                BigDecimal dishPrice = getBigDecimalValue(dishData, "price");
                BigDecimal itemPrice = dishPrice.multiply(new BigDecimal(reqItem.getQuantity()));

                OrderItem orderItem = OrderItem.builder()
                        // Order will be set later by the Facade
                        .dishId(reqItem.getDish().getId())
                        .quantity(reqItem.getQuantity())
                        .build();

                // Process options
                if (reqItem.getOrderItemOptions() != null && !reqItem.getOrderItemOptions().isEmpty()) {
                    List<OrderItemOption> itemOptions = new ArrayList<>();
                    for (ReqOrderDTO.OrderItem.OrderItemOption reqOption : reqItem.getOrderItemOptions()) {
                        if (reqOption.getMenuOption() == null || reqOption.getMenuOption().getId() == null) {
                            throw new IdInvalidException("Menu option is required");
                        }
                        Map<String, Object> menuOptionData = restaurantServiceClient
                                .getMenuOptionById(reqOption.getMenuOption().getId());
                        if (menuOptionData == null) {
                            throw new IdInvalidException(
                                    "Menu option not found with id: " + reqOption.getMenuOption().getId());
                        }

                        BigDecimal optionPrice = getBigDecimalValue(menuOptionData, "priceAdjustment");
                        String optionName = getStringValue(menuOptionData, "name");

                        OrderItemOption itemOption = OrderItemOption.builder()
                                .orderItem(orderItem)
                                .menuOptionId(reqOption.getMenuOption().getId())
                                .optionName(optionName)
                                .priceAtPurchase(optionPrice)
                                .build();
                        itemOptions.add(itemOption);
                        itemPrice = itemPrice.add(optionPrice.multiply(new BigDecimal(reqItem.getQuantity())));
                    }
                    orderItem.setOrderItemOptions(itemOptions);
                }

                orderItem.setPriceAtPurchase(itemPrice);
                subtotal = subtotal.add(itemPrice);
                orderItems.add(orderItem);
            }
        }

        context.setOrderItems(orderItems);
        context.setSubtotal(subtotal);

        super.handle(context);
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
