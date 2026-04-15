package com.eatzy.communication.service;

import com.eatzy.communication.dto.ChatMessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis service for caching chat messages.
 * Uses Redis Lists to store message history per order.
 * TTL: 7 days.
 */
@Service
public class RedisChatService {

    private static final Logger log = LoggerFactory.getLogger(RedisChatService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_KEY_PREFIX = "chat:order:";
    private static final String CHAT_KEY_SUFFIX = ":messages";
    private static final long TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days

    public RedisChatService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheMessage(ChatMessageDTO message) {
        try {
            String key = buildKey(message.getOrderId());
            String messageJson = objectMapper.writeValueAsString(message);

            redisTemplate.opsForList().rightPush(key, messageJson);
            redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);

            log.debug("💾 Cached message to Redis: order={}", message.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for caching", e);
        }
    }

    public List<ChatMessageDTO> getCachedMessages(Long orderId, int page, int size) {
        try {
            String key = buildKey(orderId);
            Long total = redisTemplate.opsForList().size(key);
            
            if (total == null || total == 0) return new ArrayList<>();

            // 0-indexed, newest first (read from end backwards)
            long start = Math.max(0, total - (page + 1) * size);
            long end = total - page * size - 1;

            if (start > end || end < 0) return new ArrayList<>();

            List<Object> rawMessages = redisTemplate.opsForList().range(key, start, end);
            if (rawMessages == null || rawMessages.isEmpty()) return new ArrayList<>();

            List<ChatMessageDTO> messages = new ArrayList<>();
            for (int i = 0; i < rawMessages.size(); i++) {
                try {
                    ChatMessageDTO msg = objectMapper.readValue(rawMessages.get(i).toString(), ChatMessageDTO.class);
                    messages.add(msg);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize message", e);
                }
            }
            return messages;
        } catch (Exception e) {
            log.error("Failed to get cached messages", e);
            return new ArrayList<>();
        }
    }

    public Long getMessageCount(Long orderId) {
        try {
            Long count = redisTemplate.opsForList().size(buildKey(orderId));
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public boolean isCached(Long orderId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(orderId)));
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteChatHistory(Long orderId) {
        try {
            redisTemplate.delete(buildKey(orderId));
            log.info("🗑️ Deleted chat history from Redis for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to delete chat history for order {}", orderId, e);
        }
    }

    private String buildKey(Long orderId) {
        return CHAT_KEY_PREFIX + orderId + CHAT_KEY_SUFFIX;
    }
}
