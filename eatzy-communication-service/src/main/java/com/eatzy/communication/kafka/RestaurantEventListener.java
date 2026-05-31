package com.eatzy.communication.kafka;

import com.eatzy.common.email.Email;
import com.eatzy.common.email.EmailFactory;
import com.eatzy.common.event.RestaurantApprovedEvent;
import com.eatzy.communication.designpattern.factory.NotificationFactory;
import com.eatzy.communication.designpattern.factory.SystemNotification;
import com.eatzy.communication.service.EmailSenderService;
import com.eatzy.communication.service.WebSocketService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Distributed Observer Pattern — Kafka Consumer for restaurant lifecycle events.
 *
 * Listens to the "restaurant-events" topic published by restaurant-service.
 * When a restaurant is approved:
 *   1. Sends a real email to the owner (via EmailSenderService + EmailFactory).
 *   2. Pushes a WebSocket notification to the owner if they are online.
 *
 * restaurant-service does NOT know this class exists — full loose coupling.
 */
@Component
@Slf4j
public class RestaurantEventListener {

    private final EmailSenderService emailSenderService;
    private final WebSocketService webSocketService;

    public RestaurantEventListener(EmailSenderService emailSenderService,
                                    WebSocketService webSocketService) {
        this.emailSenderService = emailSenderService;
        this.webSocketService = webSocketService;
    }

    @KafkaListener(topics = "restaurant-events", groupId = "communication-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleRestaurantEvents(Object rawEvent) {
        if (rawEvent instanceof org.apache.kafka.clients.consumer.ConsumerRecord) {
            rawEvent = ((org.apache.kafka.clients.consumer.ConsumerRecord<?, ?>) rawEvent).value();
        }

        log.info("📥 Received event on restaurant-events topic: {}", rawEvent);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(new JavaTimeModule());

            RestaurantApprovedEvent event = mapper.convertValue(rawEvent, RestaurantApprovedEvent.class);

            if (event == null || event.getOwnerEmail() == null) {
                log.warn("⚠️ Received null or incomplete RestaurantApprovedEvent, skipping.");
                return;
            }

            handleRestaurantApproved(event);

        } catch (Exception e) {
            log.error("❌ Error processing restaurant event: {}", e.getMessage(), e);
        }
    }

    private void handleRestaurantApproved(RestaurantApprovedEvent event) {
        log.info("🎉 Processing restaurant approved event for: {} (ID: {})",
                event.getRestaurantName(), event.getRestaurantId());

        // 1. Send email to restaurant owner via EmailFactory (template from eatzy-common)
        Email email = EmailFactory.createRestaurantApprovedEmail(
                event.getOwnerEmail(),
                event.getRestaurantName()
        );
        emailSenderService.send(email);

        // 2. Push WebSocket notification if owner is online
        try {
            SystemNotification notification = NotificationFactory.createSystemNotification(
                    event.getOwnerEmail(),
                    "🎊 Nhà hàng được duyệt!",
                    "Nhà hàng " + event.getRestaurantName() + " đã được phê duyệt. Bạn có thể bắt đầu nhận đơn hàng ngay!",
                    "SUCCESS"
            );
            webSocketService.pushNotification(notification);
            log.info("📡 WebSocket notification sent to owner: {}", event.getOwnerEmail());
        } catch (Exception e) {
            log.warn("⚠️ Failed to push WebSocket notification for restaurant {}: {}",
                    event.getRestaurantId(), e.getMessage());
            // Non-critical — email was already sent
        }
    }
}
