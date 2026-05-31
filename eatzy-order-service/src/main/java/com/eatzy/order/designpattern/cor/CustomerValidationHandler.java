package com.eatzy.order.designpattern.cor;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.adapter.AuthServiceClient;
import com.eatzy.order.designpattern.context.OrderCreationContext;
import com.eatzy.order.dto.request.ReqOrderDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CustomerValidationHandler extends OrderValidationHandler {

    private final AuthServiceClient authServiceClient;

    public CustomerValidationHandler(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    public void handle(OrderCreationContext context) throws IdInvalidException {
        ReqOrderDTO reqOrderDTO = context.getReqOrderDTO();

        // 1. Validate customer via REST (Adapter Pattern)
        if (reqOrderDTO.getCustomer() == null || reqOrderDTO.getCustomer().getId() == null) {
            throw new IdInvalidException("Customer is required");
        }
        
        Map<String, Object> customerData = authServiceClient.getUserById(reqOrderDTO.getCustomer().getId());
        if (customerData == null) {
            throw new IdInvalidException("Customer not found with id: " + reqOrderDTO.getCustomer().getId());
        }

        // Save to context
        context.setCustomerData(customerData);

        // Pass to next handler
        super.handle(context);
    }
}
