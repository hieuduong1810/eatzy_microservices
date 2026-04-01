package com.eatzy.communication.service;

import com.eatzy.communication.designpattern.adapter.AuthServiceClient;
import com.eatzy.communication.designpattern.adapter.OrderServiceClient;
import com.eatzy.communication.domain.ChatMessage;
import com.eatzy.communication.dto.ChatMessageDTO;
import com.eatzy.communication.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Business logic service for chat messages.
 * Orchestrates between Database (persistent) and Redis (cache).
 * Write: Async DB + Sync Redis
 * Read: Redis first, DB fallback
 */
@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final RedisChatService redisChatService;
    private final OrderServiceClient orderServiceClient;
    private final AuthServiceClient authServiceClient;

    public ChatMessageService(ChatMessageRepository chatMessageRepository, 
                              RedisChatService redisChatService,
                              OrderServiceClient orderServiceClient,
                              AuthServiceClient authServiceClient) {
        this.chatMessageRepository = chatMessageRepository;
        this.redisChatService = redisChatService;
        this.orderServiceClient = orderServiceClient;
        this.authServiceClient = authServiceClient;
    }

    public void saveMessage(ChatMessageDTO dto) {
        try {
            // 1. Sync Cache
            redisChatService.cacheMessage(dto);

            // 2. Async DB Save
            saveToDatabase(dto);

            log.info("💬 Saved chat message: order={}, sender={}", dto.getOrderId(), dto.getSenderId());
        } catch (Exception e) {
            log.error("Failed to save chat message", e);
        }
    }

    @Async
    protected CompletableFuture<Void> saveToDatabase(ChatMessageDTO dto) {
        try {
            ChatMessage entity = convertToEntity(dto);
            if (entity != null) {
                chatMessageRepository.save(entity);
            }
        } catch (Exception e) {
            log.error("Failed async DB save", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public List<ChatMessageDTO> getMessageHistory(Long orderId, int page, int size) {
        try {
            // Try Redis Cache First
            if (redisChatService.isCached(orderId)) {
                List<ChatMessageDTO> cached = redisChatService.getCachedMessages(orderId, page, size);
                if (!cached.isEmpty()) {
                    log.debug("🎯 CACHE HIT for order {}", orderId);
                    return cached;
                }
            }

            // Fallback to Database
            log.debug("❌ CACHE MISS: Fetching from DB for order {}", orderId);
            return getMessagesFromDatabase(orderId, page, size);
        } catch (Exception e) {
            log.error("Failed to get message history", e);
            return new ArrayList<>();
        }
    }

    public Long getMessageCount(Long orderId) {
        long count = redisChatService.getMessageCount(orderId);
        if (count > 0) return count;
        
        return chatMessageRepository.countByOrderId(orderId);
    }

    private List<ChatMessageDTO> getMessagesFromDatabase(Long orderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> dbPage = chatMessageRepository.findByOrderIdOrderBySentAtDesc(orderId, pageable);
        
        List<ChatMessageDTO> messages = dbPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Re-cache to warm it up
        if (!messages.isEmpty()) {
            // Redis needs chronologically pushed data to list properly, this simple loop might push in wrong semantic order 
            // if we blindly loop. For simplicity, we just return DB data.
            log.debug("📊 Retrieved {} messages from DB for order {}", messages.size(), orderId);
        }
        return messages;
    }

    private ChatMessage convertToEntity(ChatMessageDTO dto) {
        Map<String, Object> orderMap = orderServiceClient.getOrderById(dto.getOrderId());
        if (orderMap == null) return null;

        Long customerId = getLongValue(orderMap, "customerId");
        Long driverId = getLongValue(orderMap, "driverId");

        Long recipientId = null;
        if ("CUSTOMER".equals(dto.getSenderType()) && driverId != null) {
            recipientId = driverId;
        } else if ("DRIVER".equals(dto.getSenderType()) && customerId != null) {
            recipientId = customerId;
        }

        return ChatMessage.builder()
                .orderId(dto.getOrderId())
                .senderId(dto.getSenderId())
                .recipientId(recipientId)
                .messageContent(dto.getMessage())
                .senderType(dto.getSenderType())
                .messageType(dto.getMessageType() != null ? dto.getMessageType() : "TEXT")
                .sentAt(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now())
                .isRead(false)
                .build();
    }

    private ChatMessageDTO convertToDTO(ChatMessage entity) {
        Map<String, Object> userMap = authServiceClient.getUserById(entity.getSenderId());
        String senderName = userMap != null && userMap.containsKey("name") ? (String) userMap.get("name") : "User";

        return new ChatMessageDTO(
                entity.getOrderId(),
                entity.getSenderId(),
                senderName,
                entity.getSenderType(),
                entity.getMessageContent(),
                entity.getSentAt(),
                entity.getMessageType()
        );
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
