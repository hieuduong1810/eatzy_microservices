package com.eatzy.interaction.designpattern.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.eatzy.interaction.dto.request.ScoringEventDTO;

import java.util.HashMap;
import java.util.Map;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReviewCreatedEvent(String target, String targetName, Integer rating) {
        Map<String, Object> event = new HashMap<>();
        event.put("target", target); // "restaurant" or "driver"
        event.put("targetName", targetName);
        event.put("rating", rating);

        kafkaTemplate.send("review_created_topic", event);
    }


    public void publishReviewScoreEvent(ScoringEventDTO event, Integer rating) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "RATED");
        payload.put("userId", event.getUserId());
        payload.put("restaurantId", event.getRestaurantId());
        payload.put("restaurantTypeIds", event.getRestaurantTypeIds());
        payload.put("rating", rating);
        kafkaTemplate.send("review_score_topic", payload);
    }
}
