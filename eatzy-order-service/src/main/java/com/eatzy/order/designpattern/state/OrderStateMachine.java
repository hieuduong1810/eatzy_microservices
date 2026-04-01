package com.eatzy.order.designpattern.state;

import com.eatzy.common.exception.IdInvalidException;

/**
 * State Pattern: OrderStateMachine validates and performs state transitions.
 * Centralizes transition logic that was scattered across multiple methods in the monolith.
 */
public class OrderStateMachine {

    /**
     * Validate and return the new status after transition.
     *
     * @param currentStatus current order status string
     * @param targetStatus  target order status string
     * @return the target OrderStatus enum value
     * @throws IdInvalidException if the transition is not allowed
     */
    public static OrderStatus transition(String currentStatus, String targetStatus) throws IdInvalidException {
        OrderStatus current = parseStatus(currentStatus);
        OrderStatus target = parseStatus(targetStatus);

        if (!current.canTransitionTo(target)) {
            throw new IdInvalidException(
                    "Cannot transition order from " + current + " to " + target +
                            ". Allowed transitions from " + current + ": " + current.getAllowedTransitions());
        }

        return target;
    }

    /**
     * Validate that cancellation is allowed for the current status.
     *
     * @param currentStatus current order status string
     * @throws IdInvalidException if cancellation is not allowed
     */
    public static void validateCancellation(String currentStatus) throws IdInvalidException {
        OrderStatus current = parseStatus(currentStatus);
        if (!current.isCancellable()) {
            throw new IdInvalidException(
                    "Cannot cancel order with status: " + current +
                            ". Cancellation is only allowed for PENDING or PREPARING orders.");
        }
    }

    /**
     * Parse a string status to OrderStatus enum.
     */
    public static OrderStatus parseStatus(String status) throws IdInvalidException {
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IdInvalidException("Invalid order status: " + status);
        }
    }
}
