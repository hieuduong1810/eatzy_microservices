package com.eatzy.communication.controller;

import com.eatzy.communication.designpattern.adapter.OrderServiceClient;
import com.eatzy.communication.designpattern.factory.ChatNotification;
import com.eatzy.communication.designpattern.factory.NotificationFactory;
import com.eatzy.communication.dto.ChatMessageDTO;
import com.eatzy.communication.service.ChatMessageService;
import com.eatzy.communication.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

/**
 * Controller for handling WebSocket STOMP chat messages between Driver and
 * Customer.
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final WebSocketService webSocketService;
    private final ChatMessageService chatMessageService;
    private final OrderServiceClient orderServiceClient;

    public ChatController(WebSocketService webSocketService, ChatMessageService chatMessageService,
            OrderServiceClient orderServiceClient) {
        this.webSocketService = webSocketService;
        this.chatMessageService = chatMessageService;
        this.orderServiceClient = orderServiceClient;
    }

    @MessageMapping("/chat/{orderId}")
    public void handleChatMessage(@DestinationVariable("orderId") Long orderId, @Payload ChatMessageDTO message,
            Principal principal) {
        String authenticatedEmail = principal != null ? principal.getName() : null;
        if (authenticatedEmail == null)
            return;

        message.setTimestamp(Instant.now());
        message.setOrderId(orderId);

        Map<String, Object> orderMap = orderServiceClient.getOrderById(orderId);
        if (orderMap == null)
            return;

        // Check auth and determine routing
        String customerEmail = getNestedString(orderMap, "customer", "email");
        String driverEmail = getNestedString(orderMap, "driver", "email");

        boolean isCustomer = customerEmail != null && customerEmail.equals(authenticatedEmail);
        boolean isDriver = driverEmail != null && driverEmail.equals(authenticatedEmail);

        if (!isCustomer && !isDriver) {
            log.warn("Unauthorized chat access attempt for order {}", orderId);
            return;
        }

        // Always set sender info from order data (server-side, not trusting client)
        if (isCustomer) {
            message.setSenderId(getNestedLong(orderMap, "customer", "id"));
            message.setRecipientId(getNestedLong(orderMap, "driver", "id"));
            if (message.getSenderName() == null || message.getSenderName().isEmpty()) {
                message.setSenderName(getNestedString(orderMap, "customer", "name"));
            }
            message.setSenderType("CUSTOMER");
        } else {
            message.setSenderId(getNestedLong(orderMap, "driver", "id"));
            message.setRecipientId(getNestedLong(orderMap, "customer", "id"));
            if (message.getSenderName() == null || message.getSenderName().isEmpty()) {
                message.setSenderName(getNestedString(orderMap, "driver", "name"));
            }
            message.setSenderType("DRIVER");
        }

        // Push via Factory
        if (customerEmail != null && message.getSenderType().equals("DRIVER")) {
            // Send to Customer
            ChatNotification notification = NotificationFactory.createChatNotification(
                    customerEmail, orderId, message.getSenderId(), message.getSenderName(),
                    message.getSenderType(), message.getMessage());
            webSocketService.pushNotification(notification);
        }

        if (driverEmail != null && message.getSenderType().equals("CUSTOMER")) {
            // Send to Driver
            ChatNotification notification = NotificationFactory.createChatNotification(
                    driverEmail, orderId, message.getSenderId(), message.getSenderName(),
                    message.getSenderType(), message.getMessage());
            webSocketService.pushNotification(notification);
        }

        // Persist message
        chatMessageService.saveMessage(message);
    }

    @MessageMapping("/typing/{orderId}")
    public void handleTypingIndicator(@DestinationVariable("orderId") Long orderId, @Payload ChatMessageDTO message,
            Principal principal) {
        String authenticatedEmail = principal != null ? principal.getName() : null;
        if (authenticatedEmail == null)
            return;

        Map<String, Object> orderMap = orderServiceClient.getOrderById(orderId);
        if (orderMap == null)
            return;

        String customerEmail = getNestedString(orderMap, "customer", "email");
        String driverEmail = getNestedString(orderMap, "driver", "email");

        boolean isCustomer = customerEmail != null && customerEmail.equals(authenticatedEmail);
        boolean isDriver = driverEmail != null && driverEmail.equals(authenticatedEmail);

        String targetEmail = null;
        if (isCustomer) {
            targetEmail = driverEmail;
            message.setSenderType("CUSTOMER"); // Ensure correct mapping
        } else if (isDriver) {
            targetEmail = customerEmail;
            message.setSenderType("DRIVER");
        }

        if (targetEmail != null) {
            // Custom destination for typing: /queue/chat/order/{orderId}/typing
            webSocketService.sendToUser(targetEmail, "/queue/chat/order/" + orderId + "/typing", message);
        }
    }

    private String getNestedString(Map<String, Object> map, String keyPath1, String keyPath2) {
        if (!map.containsKey(keyPath1))
            return null;
        Object nested = map.get(keyPath1);
        if (nested instanceof Map) {
            Map<?, ?> nestedMap = (Map<?, ?>) nested;
            if (nestedMap.containsKey(keyPath2)) {
                return (String) nestedMap.get(keyPath2);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Long getNestedLong(Map<String, Object> map, String keyPath1, String keyPath2) {
        Object nested = map.get(keyPath1);
        if (nested instanceof Map) {
            Object val = ((Map<String, Object>) nested).get(keyPath2);
            if (val instanceof Number) return ((Number) val).longValue();
            if (val != null) return Long.parseLong(val.toString());
        }
        return null;
    }
}
