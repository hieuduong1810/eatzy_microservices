package com.eatzy.order.designpattern.state;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State Pattern: Enum defining all valid order statuses and allowed transitions.
 * Each status knows which statuses it can transition to.
 *
 * Order lifecycle:
 * PENDING → PREPARING → DRIVER_ASSIGNED → READY → PICKED_UP → ARRIVED → DELIVERED
 * PENDING → REJECTED (by customer or restaurant)
 * PREPARING → REJECTED (by customer)
 */
public enum OrderStatus {
    PENDING,
    PREPARING,
    DRIVER_ASSIGNED,
    READY,
    PICKED_UP,
    ARRIVED,
    DELIVERED,
    REJECTED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING,          EnumSet.of(PREPARING, REJECTED),
            PREPARING,        EnumSet.of(DRIVER_ASSIGNED, READY, REJECTED),
            DRIVER_ASSIGNED,  EnumSet.of(READY, PICKED_UP, REJECTED),
            READY,            EnumSet.of(PICKED_UP),
            PICKED_UP,        EnumSet.of(ARRIVED),
            ARRIVED,          EnumSet.of(DELIVERED),
            DELIVERED,        EnumSet.noneOf(OrderStatus.class),
            REJECTED,         EnumSet.noneOf(OrderStatus.class)
    );

    /**
     * Check if transitioning from this status to the target status is allowed.
     */
    public boolean canTransitionTo(OrderStatus target) {
        Set<OrderStatus> allowed = TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    /**
     * Get all statuses this status can transition to.
     */
    public Set<OrderStatus> getAllowedTransitions() {
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class));
    }

    /**
     * Check if this status is a terminal state (no further transitions possible).
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == REJECTED;
    }

    /**
     * Check if cancellation is allowed in this status.
     */
    public boolean isCancellable() {
        return this == PENDING || this == PREPARING;
    }
}
