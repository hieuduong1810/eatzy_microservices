package com.eatzy.order.designpattern.cor;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.context.OrderCreationContext;

/**
 * Base abstract class for the Chain of Responsibility Pattern.
 */
public abstract class OrderValidationHandler {
    protected OrderValidationHandler next;

    public OrderValidationHandler setNext(OrderValidationHandler next) {
        this.next = next;
        return next;
    }

    public void handle(OrderCreationContext context) throws IdInvalidException {
        if (next != null) {
            next.handle(context);
        }
    }
}
