package com.eatzy.order.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing driver rejections using Redis SET
 * Key pattern: "order:{orderId}:rejected_drivers"
 * TTL: 24 hours (auto cleanup)
 *
 * Ported from eatzy_backend RedisRejectionService.
 */
@Service
public class RedisRejectionService {
    private static final Logger log = LoggerFactory.getLogger(RedisRejectionService.class);

    private static final String REJECTION_KEY_PREFIX = "order:";
    private static final String REJECTION_KEY_SUFFIX = ":rejected_drivers";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRejectionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add a driver to the rejected drivers set for an order
     */
    public void addRejectedDriver(Long orderId, Long driverId) {
        String key = buildKey(orderId);
        redisTemplate.opsForSet().add(key, driverId.toString());
        redisTemplate.expire(key, TTL);
        log.debug("Added driver {} to rejected list for order {} (TTL: {} hours)",
                driverId, orderId, TTL.toHours());
    }

    /**
     * Get all rejected driver IDs for an order
     */
    public List<Long> getRejectedDriverIds(Long orderId) {
        String key = buildKey(orderId);
        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null || members.isEmpty()) {
            return new ArrayList<>();
        }

        return members.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    /**
     * Check if a driver has rejected an order
     */
    public boolean hasDriverRejected(Long orderId, Long driverId) {
        String key = buildKey(orderId);
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(key, driverId.toString()));
    }

    /**
     * Remove all rejections for an order (cleanup)
     */
    public void clearOrderRejections(Long orderId) {
        String key = buildKey(orderId);
        redisTemplate.delete(key);
        log.debug("Cleared all rejections for order {}", orderId);
    }

    private String buildKey(Long orderId) {
        return REJECTION_KEY_PREFIX + orderId + REJECTION_KEY_SUFFIX;
    }
}
