package com.eatzy.restaurant.kafka;

import com.eatzy.common.event.RestaurantApprovedEvent;
import com.eatzy.restaurant.domain.Restaurant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RestaurantEventProducer {
    private static final Logger log = LoggerFactory.getLogger(RestaurantEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RestaurantEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSearchEvent(String action, Long userId, Long restaurantId, List<Long> typeIds) {
        if (userId == null) return;
        Map<String, Object> event = new HashMap<>();
        event.put("action", action);
        event.put("userId", userId);
        event.put("restaurantId", restaurantId);
        event.put("restaurantTypeIds", typeIds);

        log.info("📤 Publishing Interaction Event for Restaurant {}: {}", action, event);
        kafkaTemplate.send("search_events_topic", event);
    }

    /**
     * Distributed Observer Pattern:
     * Publishes a RestaurantApprovedEvent to Kafka so that communication-service
     * can send email + WebSocket notification to the owner.
     * restaurant-service does NOT know about communication-service — loose coupling.
     */
    public void publishRestaurantApproved(Restaurant restaurant) {
        if (restaurant == null) return;

        RestaurantApprovedEvent event = new RestaurantApprovedEvent(
                restaurant.getId(),
                restaurant.getName(),
                // ownerEmail: use placeholder — communication-service will resolve via auth-service
                "owner-" + restaurant.getOwnerId() + "@placeholder.com",
                "Owner"
        );

        log.info("📤 Publishing RestaurantApprovedEvent for restaurant {} (ID: {})",
                restaurant.getName(), restaurant.getId());
        kafkaTemplate.send("restaurant-events", event);
    }
}

