package com.eatzy.order.designpattern.cor;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.order.designpattern.context.OrderCreationContext;
import com.eatzy.order.dto.request.ReqOrderDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RestaurantValidationHandler extends OrderValidationHandler {

    private final RestaurantServiceClient restaurantServiceClient;

    public RestaurantValidationHandler(RestaurantServiceClient restaurantServiceClient) {
        this.restaurantServiceClient = restaurantServiceClient;
    }

    @Override
    public void handle(OrderCreationContext context) throws IdInvalidException {
        ReqOrderDTO reqOrderDTO = context.getReqOrderDTO();

        // 2. Validate restaurant via REST (Adapter Pattern)
        if (reqOrderDTO.getRestaurant() == null || reqOrderDTO.getRestaurant().getId() == null) {
            throw new IdInvalidException("Restaurant is required");
        }
        
        Map<String, Object> restData = restaurantServiceClient.getRestaurantById(reqOrderDTO.getRestaurant().getId());
        if (restData == null) {
            throw new IdInvalidException("Restaurant not found with id: " + reqOrderDTO.getRestaurant().getId());
        }

        // Save to context
        context.setRestaurantData(restData);

        // Pass to next handler
        super.handle(context);
    }
}
