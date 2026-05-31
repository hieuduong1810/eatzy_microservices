package com.eatzy.common.event;

import java.io.Serializable;

/**
 * Kafka event DTO published by restaurant-service when a restaurant is approved.
 * Consumed by communication-service to send email and WebSocket notification to owner.
 *
 * Follows the same pattern as UserStatusChangedEvent.
 */
public class RestaurantApprovedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long restaurantId;
    private String restaurantName;
    private String ownerEmail;
    private String ownerName;

    public RestaurantApprovedEvent() {
    }

    public RestaurantApprovedEvent(Long restaurantId, String restaurantName,
                                    String ownerEmail, String ownerName) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @Override
    public String toString() {
        return "RestaurantApprovedEvent{" +
                "restaurantId=" + restaurantId +
                ", restaurantName='" + restaurantName + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", ownerName='" + ownerName + '\'' +
                '}';
    }
}
