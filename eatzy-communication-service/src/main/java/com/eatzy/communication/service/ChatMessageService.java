package com.eatzy.communication.service;

import com.eatzy.communication.dto.ChatMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic service for chat messages.
 * Uses ONLY Redis for in-memory persistence.
 * Note: chat history will be automatically deleted when order is delivered.
 */
@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final RedisChatService redisChatService;

    public ChatMessageService(RedisChatService redisChatService) {
        this.redisChatService = redisChatService;
    }

    public void saveMessage(ChatMessageDTO dto) {
        try {
            // 1. Sync Cache (Only Redis)
            redisChatService.cacheMessage(dto);
            log.info("💬 Saved chat message to Redis: order={}, sender={}, recipient={}",
                    dto.getOrderId(), dto.getSenderId(), dto.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to save chat message", e);
        }
    }

    public List<ChatMessageDTO> getMessageHistory(Long orderId, int page, int size) {
        try {
            List<ChatMessageDTO> cached = redisChatService.getCachedMessages(orderId, page, size);
            if (!cached.isEmpty()) {
                log.debug("🎯 Retrieved {} messages from Redis for order {}", cached.size(), orderId);
            }
            return cached;
        } catch (Exception e) {
            log.error("Failed to get message history from Redis", e);
            return new ArrayList<>();
        }
    }

    public Long getMessageCount(Long orderId) {
        return redisChatService.getMessageCount(orderId);
    }
}
