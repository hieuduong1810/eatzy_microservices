package com.eatzy.auth.kafka;

import com.eatzy.common.event.UserStatusChangedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthMessagePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String USER_EVENTS_TOPIC = "user-events";

    public AuthMessagePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserStatusChangedEvent(UserStatusChangedEvent event) {
        kafkaTemplate.send(USER_EVENTS_TOPIC, event.getUserId().toString(), event);
        System.out.println("Published user status changed event: " + event);
    }
}
