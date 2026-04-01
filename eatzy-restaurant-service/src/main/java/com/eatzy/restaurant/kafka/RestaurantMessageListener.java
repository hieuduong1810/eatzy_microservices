package com.eatzy.restaurant.kafka;

import com.eatzy.common.event.UserStatusChangedEvent;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.repository.RestaurantRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class RestaurantMessageListener {

    private final RestaurantRepository restaurantRepository;

    public RestaurantMessageListener(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @KafkaListener(topics = "user-events", groupId = "restaurant-group")
    public void handleUserStatusChangedEvent(UserStatusChangedEvent event) {
        System.out.println("Received user status changed event: " + event);

        // If user is deactivated and they are a RESTAURANT owner, close their restaurant
        if (Boolean.FALSE.equals(event.getIsActive()) && "RESTAURANT".equals(event.getRole())) {
            Restaurant restaurant = restaurantRepository.findByOwnerId(event.getUserId()).orElse(null);
            if (restaurant != null) {
                restaurant.setStatus("CLOSED");
                restaurantRepository.save(restaurant);
                System.out.println("Restaurant closed due to owner account deactivation: " + restaurant.getId());
            }
        }
    }
}
