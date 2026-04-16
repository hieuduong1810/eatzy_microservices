package com.eatzy.communication.controller;

import com.eatzy.communication.designpattern.adapter.AuthServiceClient;
import com.eatzy.communication.designpattern.adapter.OrderServiceClient;
import com.eatzy.communication.designpattern.factory.DriverLocationNotification;
import com.eatzy.communication.designpattern.factory.NotificationFactory;
import com.eatzy.communication.dto.DriverLocationUpdate;
import com.eatzy.common.service.RedisGeoService;
import com.eatzy.communication.service.WebSocketService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

/**
 * Controller handling Driver GPS tracking via STOMP WebSocket.
 */
@Controller
public class DriverLocationController {

    private final RedisGeoService redisGeoService;
    private final WebSocketService webSocketService;
    private final AuthServiceClient authServiceClient;
    private final OrderServiceClient orderServiceClient;

    public DriverLocationController(RedisGeoService redisGeoService, WebSocketService webSocketService,
            AuthServiceClient authServiceClient, OrderServiceClient orderServiceClient) {
        this.redisGeoService = redisGeoService;
        this.webSocketService = webSocketService;
        this.authServiceClient = authServiceClient;
        this.orderServiceClient = orderServiceClient;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DriverLocationController.class);

    @MessageMapping("/driver/location")
    public void updateDriverLocation(@Payload DriverLocationUpdate locationUpdate, Principal principal) {
        String driverEmail = principal != null ? principal.getName() : null;
        if (driverEmail == null) return;

        locationUpdate.setTimestamp(Instant.now());

        Long driverId = null;
        try {
            Map<String, Object> userData = authServiceClient.getUserByEmail(driverEmail);
            if (userData != null && userData.containsKey("id")) {
                driverId = Long.parseLong(userData.get("id").toString());
            }
        } catch (Exception e) {
            log.warn("Failed to get driver ID for email {}: {}", driverEmail, e.getMessage());
        }

        if (driverId == null) return;

        redisGeoService.updateDriverLocation(driverId, locationUpdate.getLatitude(), locationUpdate.getLongitude());

        Map<String, Object> orderMap = null;
        try {
            orderMap = orderServiceClient.getActiveOrderByDriverId(driverId);
        } catch (Exception e) {
            // No active order found or service down
        }

        if (orderMap != null && !orderMap.isEmpty()) {
            Object customerObj = orderMap.get("customer");
            if (customerObj instanceof Map) {
                String customerEmail = (String) ((Map<?, ?>) customerObj).get("email");
                if (customerEmail != null) {
                    DriverLocationNotification n = NotificationFactory.createDriverLocationNotification(
                            customerEmail, locationUpdate.getLatitude(), locationUpdate.getLongitude());
                    webSocketService.pushNotification(n);
                }
            }
        }
    }
}
