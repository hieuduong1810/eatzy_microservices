package com.eatzy.common.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for handling geospatial data in Redis.
 * Used for storing and querying driver locations.
 * Ported from eatzy_backend RedisGeoService.
 * 
 * Shared via eatzy-common so both auth-service (write) and order-service (read)
 * can interact with the same Redis GEO data.
 */
@Service
@ConditionalOnBean(RedisTemplate.class)
public class RedisGeoService {
    private static final Logger log = LoggerFactory.getLogger(RedisGeoService.class);

    private static final String DRIVER_LOCATION_KEY = "geo:drivers:active";
    private final GeoOperations<String, Object> geoOps;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisGeoService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.geoOps = redisTemplate.opsForGeo();
    }

    /**
     * Add or update driver location
     *
     * @param driverId  Driver's user ID
     * @param latitude  Latitude coordinate
     * @param longitude Longitude coordinate
     */
    public void updateDriverLocation(Long driverId, BigDecimal latitude, BigDecimal longitude) {
        try {
            // Redis GEO uses (longitude, latitude)
            Point point = new Point(longitude.doubleValue(), latitude.doubleValue());
            geoOps.add(DRIVER_LOCATION_KEY, point, driverId.toString());

            // Set expiration for the entire key (24 hours)
            redisTemplate.expire(DRIVER_LOCATION_KEY, 24, TimeUnit.HOURS);

            log.debug("📍 Updated driver {} location in Redis GEO: lat={}, lng={}",
                    driverId, latitude, longitude);
        } catch (Exception e) {
            log.error("Failed to update driver location in Redis", e);
        }
    }

    /**
     * Get driver's current location from Redis
     *
     * @param driverId Driver's user ID
     * @return Point with (longitude, latitude) or null if not found
     */
    public Point getDriverLocation(Long driverId) {
        try {
            List<Point> positions = geoOps.position(DRIVER_LOCATION_KEY, driverId.toString());
            if (positions != null && !positions.isEmpty()) {
                return positions.get(0);
            }
        } catch (Exception e) {
            log.error("Failed to get driver location from Redis", e);
        }
        return null;
    }

    /**
     * Find nearby drivers within radius
     *
     * @param latitude   Center latitude
     * @param longitude  Center longitude
     * @param radiusInKm Search radius in kilometers
     * @param limit      Maximum number of results
     * @return GeoResults with driver IDs and distances
     */
    @SuppressWarnings("deprecation")
    public GeoResults<GeoLocation<Object>> findNearbyDrivers(
            BigDecimal latitude, BigDecimal longitude, Double radiusInKm, Integer limit) {
        try {
            Point center = new Point(longitude.doubleValue(), latitude.doubleValue());
            Distance radius = new Distance(radiusInKm, DistanceUnit.KILOMETERS);
            Circle within = new Circle(center, radius);

            GeoRadiusCommandArgs args = GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending();

            if (limit != null && limit > 0) {
                args = args.limit(limit);
            }

            return geoOps.radius(DRIVER_LOCATION_KEY, within, args);
        } catch (Exception e) {
            log.error("Failed to find nearby drivers", e);
            return null;
        }
    }

    /**
     * Remove driver location from Redis (when driver goes offline)
     *
     * @param driverId Driver's user ID
     */
    public void removeDriverLocation(Long driverId) {
        try {
            geoOps.remove(DRIVER_LOCATION_KEY, driverId.toString());
            log.debug("🗑️ Removed driver {} location from Redis GEO", driverId);
        } catch (Exception e) {
            log.error("Failed to remove driver location from Redis", e);
        }
    }

    /**
     * Get total count of active drivers in Redis
     */
    public Long getActiveDriverCount() {
        try {
            return redisTemplate.opsForZSet().size(DRIVER_LOCATION_KEY);
        } catch (Exception e) {
            log.error("Failed to get active driver count", e);
            return 0L;
        }
    }
}
