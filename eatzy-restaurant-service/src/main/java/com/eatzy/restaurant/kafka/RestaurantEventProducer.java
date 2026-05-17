package com.eatzy.restaurant.kafka;

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
}
