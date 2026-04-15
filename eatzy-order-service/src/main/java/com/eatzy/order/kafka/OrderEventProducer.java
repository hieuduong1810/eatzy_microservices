package com.eatzy.order.kafka;

import com.eatzy.common.event.OrderCreatedEvent;
import com.eatzy.common.event.OrderDeliveredEvent;
import com.eatzy.common.event.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Observer/Pub-Sub Pattern: Publishes order events to Kafka topics.
 * Notification Service (Domain 6) will consume these events and push WebSocket to clients.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("📤 Publishing OrderCreatedEvent: {}", event);
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, "order-created", event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("📤 Publishing OrderStatusChangedEvent: {}", event);
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, "order-status-changed", event);
    }

    public void publishOrderDelivered(OrderDeliveredEvent event) {
        log.info("📤 Publishing OrderDeliveredEvent: {}", event);
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, "order-delivered", event);
    }

    public void publishTrackPlaceOrder(Long customerId, Long restaurantId) {
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("action", "PLACED");
        event.put("userId", customerId);
        event.put("restaurantId", restaurantId);
        log.info("📤 Publishing Interaction Event for Scoring: {}", event);
        kafkaTemplate.send("order_events_topic", event);
    }

    public void publishDriverOnlineEvent(com.eatzy.common.event.DriverOnlineEvent event) {
        log.info("📤 Publishing DriverOnlineEvent from order-service: {}", event);
        kafkaTemplate.send("driver-events", event);
    }
}
