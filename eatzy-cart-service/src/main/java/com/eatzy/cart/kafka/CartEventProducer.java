package com.eatzy.cart.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CartEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CartEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CartEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishItemAddedEvent(Long customerId, Long restaurantId, List<Long> restaurantTypeIds) {
        Map<String, Object> event = new HashMap<>();
        event.put("action", "ITEM_ADDED");
        event.put("userId", customerId);
        event.put("restaurantId", restaurantId);
        event.put("restaurantTypeIds", restaurantTypeIds);
        
        log.info("📤 Publishing Interaction Event for Cart: {}", event);
        kafkaTemplate.send("cart_events_topic", event);
    }
}
