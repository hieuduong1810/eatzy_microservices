package com.eatzy.auth.kafka;

import com.eatzy.common.event.DriverOnlineEvent;
import com.eatzy.common.event.UserStatusChangedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthMessagePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String DRIVER_EVENTS_TOPIC = "driver-events";

    public AuthMessagePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserStatusChangedEvent(UserStatusChangedEvent event) {
        kafkaTemplate.send(USER_EVENTS_TOPIC, event.getUserId().toString(), event);
        System.out.println("Published user status changed event: " + event);
    }

    /**
     * Publish event when driver goes online.
     * Order service will consume this to assign waiting orders.
     * Matches eatzy_backend: goOnline triggers search for unassigned PREPARING orders.
     */
    public void publishDriverOnlineEvent(DriverOnlineEvent event) {
        kafkaTemplate.send(DRIVER_EVENTS_TOPIC, event.getDriverId().toString(), event);
        System.out.println("Published driver online event: " + event);
    }
}
