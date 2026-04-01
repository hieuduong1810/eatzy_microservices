package com.eatzy.interaction.designpattern.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
}
