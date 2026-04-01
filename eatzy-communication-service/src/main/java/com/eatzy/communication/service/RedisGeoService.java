package com.eatzy.communication.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Service to store realtime driver locations using Redis GEO.
 */
@Service
public class RedisGeoService {

    private static final Logger log = LoggerFactory.getLogger(RedisGeoService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String DRIVER_GEO_KEY = "driver:locations";
    private static final long LOCATION_TTL_MINUTES = 60; // Auto-expire after 1 hour

    public RedisGeoService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateDriverLocation(Long driverId, BigDecimal latitude, BigDecimal longitude) {
        try {
            redisTemplate.opsForGeo().add(
                    DRIVER_GEO_KEY, 
                    new Point(longitude.doubleValue(), latitude.doubleValue()), 
                    driverId.toString()
            );
            // Ensure TTL so offline drivers are cleared
            redisTemplate.expire(DRIVER_GEO_KEY, LOCATION_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.trace("📍 Updated GEO for driver {}: [{}, {}]", driverId, latitude, longitude);
        } catch (Exception e) {
            log.error("Failed to update driver location in Redis", e);
        }
    }
}
