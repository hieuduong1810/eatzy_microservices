package com.eatzy.auth.kafka;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.eatzy.auth.event.UserStatusLocalEvent;
import com.eatzy.common.event.UserStatusChangedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Concrete Observer in the Observer Pattern.
 * Listens to purely internal UserStatusLocalEvent and bridges it to Kafka.
 */
@Component
@Slf4j
public class UserEventKafkaObserver {

    private final AuthMessagePublisher authMessagePublisher;

    public UserEventKafkaObserver(AuthMessagePublisher authMessagePublisher) {
        this.authMessagePublisher = authMessagePublisher;
    }

    /**
     * Handle the event only after the database transaction commits successfully.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserStatusChanged(UserStatusLocalEvent localEvent) {
        log.info("Observer observed user status change for user ID: {}", localEvent.getUserId());
        
        // Map Local Event to Common Event (DTO for Kafka)
        UserStatusChangedEvent kafkaEvent = new UserStatusChangedEvent(
            localEvent.getUserId(),
            localEvent.getRole(),
            localEvent.getIsActive()
        );
        
        // Publish via Kafka
        authMessagePublisher.publishUserStatusChangedEvent(kafkaEvent);
    }
}
