package com.eatzy.order.service;

import com.eatzy.order.designpattern.adapter.AuthServiceClient;
import com.eatzy.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * Service to calculate dynamic pricing surge multiplier based on:
 * 1. Weather conditions (from WeatherService)
 * 2. Peak hours (lunch: 11-13h, dinner: 18-20h)
 * 3. Supply/Demand ratio (available drivers vs pending orders)
 * 
 * Formula: K_surge = min(M_weather × M_peak × M_supply, MAX_SURGE)
 */
@Service
@Slf4j
public class DynamicPricingService {

    private static final BigDecimal MAX_SURGE_MULTIPLIER = new BigDecimal("2.5");
    private static final BigDecimal MIN_SURGE_MULTIPLIER = BigDecimal.ONE;

    // Peak hour multiplier
    private static final BigDecimal PEAK_HOUR_MULTIPLIER = new BigDecimal("1.2");

    // Supply/demand multiplier range
    private static final BigDecimal MIN_SUPPLY_MULTIPLIER = BigDecimal.ONE;
    private static final BigDecimal MAX_SUPPLY_MULTIPLIER = new BigDecimal("2.0");

    // Order statuses that need drivers
    private static final List<String> PENDING_ORDER_STATUSES = Arrays.asList("PENDING", "PREPARING");

    private final WeatherService weatherService;
    private final OrderRepository orderRepository;
    private final AuthServiceClient authServiceClient;

    public DynamicPricingService(
            WeatherService weatherService,
            OrderRepository orderRepository,
            AuthServiceClient authServiceClient) {
        this.weatherService = weatherService;
        this.orderRepository = orderRepository;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Calculate the combined surge multiplier for dynamic pricing.
     * 
     * @param latitude  Location latitude (restaurant location)
     * @param longitude Location longitude (restaurant location)
     * @return Combined surge multiplier (capped at MAX_SURGE)
     */
    public BigDecimal getSurgeMultiplier(BigDecimal latitude, BigDecimal longitude) {
        // Get individual multipliers
        BigDecimal weatherMultiplier = getWeatherMultiplier(latitude, longitude);
        BigDecimal peakMultiplier = getPeakHourMultiplier();
        BigDecimal supplyDemandMultiplier = getSupplyDemandMultiplier(latitude, longitude);

        // Calculate combined surge: M_weather × M_peak × M_supply
        BigDecimal combinedSurge = weatherMultiplier
                .multiply(peakMultiplier)
                .multiply(supplyDemandMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // Cap at maximum surge multiplier
        if (combinedSurge.compareTo(MAX_SURGE_MULTIPLIER) > 0) {
            combinedSurge = MAX_SURGE_MULTIPLIER;
        }

        // Ensure minimum multiplier is 1.0
        if (combinedSurge.compareTo(MIN_SURGE_MULTIPLIER) < 0) {
            combinedSurge = MIN_SURGE_MULTIPLIER;
        }

        log.info("💰 Dynamic Pricing - Weather: {}, Peak: {}, Supply/Demand: {} => Combined Surge: {}",
                weatherMultiplier, peakMultiplier, supplyDemandMultiplier, combinedSurge);

        return combinedSurge;
    }

    /**
     * Get weather-based multiplier from WeatherService
     */
    private BigDecimal getWeatherMultiplier(BigDecimal latitude, BigDecimal longitude) {
        try {
            return weatherService.getWeatherMultiplier(latitude, longitude);
        } catch (Exception e) {
            log.info("Failed to get weather multiplier, using default 1.0: {}", e.getMessage());
            return BigDecimal.ONE;
        }
    }

    /**
     * Calculate peak hour multiplier based on current time.
     * Peak hours: 11:00-13:00 (lunch), 18:00-20:00 (dinner)
     * 
     * @return Peak hour multiplier (1.0 or 1.2)
     */
    public BigDecimal getPeakHourMultiplier() {
        // Use Vietnam timezone (UTC+7)
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int hour = now.getHour();

        boolean isPeakHour = (hour >= 11 && hour < 13) || (hour >= 18 && hour < 20);

        if (isPeakHour) {
            log.info("Current time {} is peak hour, applying multiplier {}", now, PEAK_HOUR_MULTIPLIER);
            return PEAK_HOUR_MULTIPLIER;
        }

        return BigDecimal.ONE;
    }

    /**
     * Calculate supply/demand multiplier based on ratio of pending orders to
     * available drivers.
     * 
     * Formula: ratio = pendingOrders / availableDrivers
     * - ratio <= 1: multiplier = 1.0 (balanced or oversupply)
     * - ratio > 1: multiplier scales up to 2.0 (shortage)
     * 
     * Note: Both drivers and orders are counted GLOBALLY for consistency.
     * 
     * @param latitude  Not used (kept for API compatibility)
     * @param longitude Not used (kept for API compatibility)
     * @return Supply/demand multiplier (1.0 to 2.0)
     */
    public BigDecimal getSupplyDemandMultiplier(BigDecimal latitude, BigDecimal longitude) {
        try {
            // Count ALL available drivers from auth-service
            long availableDrivers = authServiceClient.countDriversByStatus("AVAILABLE");

            // Count ALL pending orders globally (CONFIRMED, PREPARING, READY without
            // driver)
            long pendingOrders = orderRepository.countByOrderStatusInAndDriverIdIsNull(PENDING_ORDER_STATUSES);

            log.info("📊 Supply/Demand - Global: {} pending orders, {} available drivers",
                    pendingOrders, availableDrivers);

            // If no drivers available, return max multiplier
            if (availableDrivers == 0) {
                log.info("No active drivers in system, applying max supply/demand multiplier");
                return MAX_SUPPLY_MULTIPLIER;
            }

            // If no pending orders, no surge needed
            if (pendingOrders == 0) {
                return MIN_SUPPLY_MULTIPLIER;
            }

            // Calculate ratio: pendingOrders / availableDrivers
            double ratio = (double) pendingOrders / availableDrivers;

            // ratio <= 1: enough drivers for all orders → multiplier = 1.0
            if (ratio <= 1.0) {
                log.info("Balanced supply/demand (ratio: {:.2f}), multiplier: 1.0", ratio);
                return MIN_SUPPLY_MULTIPLIER;
            }

            // ratio > 1: more orders than drivers → scale up multiplier
            // Linear scale: ratio 1 = 1.0x, ratio 3+ = 2.0x
            // Formula: multiplier = 1.0 + (ratio - 1) * 0.5, capped at 2.0
            double multiplier = 1.0 + (ratio - 1.0) * 0.5;
            if (multiplier > MAX_SUPPLY_MULTIPLIER.doubleValue()) {
                multiplier = MAX_SUPPLY_MULTIPLIER.doubleValue();
            }

            BigDecimal result = new BigDecimal(multiplier).setScale(2, RoundingMode.HALF_UP);
            log.info("📊 Supply/Demand - {} orders / {} drivers = {:.2f} => multiplier: {}",
                    pendingOrders, availableDrivers, ratio, result);
            return result;

        } catch (Exception e) {
            log.info("Failed to calculate supply/demand multiplier, using default 1.0: {}", e.getMessage());
            return MIN_SUPPLY_MULTIPLIER;
        }
    }

    /**
     * Get a breakdown of all pricing factors for debugging/display purposes
     */
    public PricingBreakdown getPricingBreakdown(BigDecimal latitude, BigDecimal longitude) {
        BigDecimal weatherMultiplier = getWeatherMultiplier(latitude, longitude);
        BigDecimal peakMultiplier = getPeakHourMultiplier();
        BigDecimal supplyDemandMultiplier = getSupplyDemandMultiplier(latitude, longitude);
        BigDecimal combinedSurge = getSurgeMultiplier(latitude, longitude);

        WeatherService.WeatherData weather = weatherService.getCurrentWeather(latitude, longitude);

        return new PricingBreakdown(
                weatherMultiplier,
                peakMultiplier,
                supplyDemandMultiplier,
                combinedSurge,
                weather != null ? weather.getDescription() : "Unknown");
    }

    /**
     * Pricing breakdown DTO for API responses
     */
    public static class PricingBreakdown {
        private final BigDecimal weatherMultiplier;
        private final BigDecimal peakHourMultiplier;
        private final BigDecimal supplyDemandMultiplier;
        private final BigDecimal totalSurgeMultiplier;
        private final String weatherDescription;

        public PricingBreakdown(BigDecimal weatherMultiplier, BigDecimal peakHourMultiplier,
                BigDecimal supplyDemandMultiplier, BigDecimal totalSurgeMultiplier, String weatherDescription) {
            this.weatherMultiplier = weatherMultiplier;
            this.peakHourMultiplier = peakHourMultiplier;
            this.supplyDemandMultiplier = supplyDemandMultiplier;
            this.totalSurgeMultiplier = totalSurgeMultiplier;
            this.weatherDescription = weatherDescription;
        }

        public BigDecimal getWeatherMultiplier() {
            return weatherMultiplier;
        }

        public BigDecimal getPeakHourMultiplier() {
            return peakHourMultiplier;
        }

        public BigDecimal getSupplyDemandMultiplier() {
            return supplyDemandMultiplier;
        }

        public BigDecimal getTotalSurgeMultiplier() {
            return totalSurgeMultiplier;
        }

        public String getWeatherDescription() {
            return weatherDescription;
        }
    }
}
