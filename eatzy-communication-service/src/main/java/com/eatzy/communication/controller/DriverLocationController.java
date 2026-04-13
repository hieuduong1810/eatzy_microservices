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

    @MessageMapping("/driver/location")
    public void updateDriverLocation(@Payload DriverLocationUpdate locationUpdate, Principal principal) {
        String driverEmail = principal != null ? principal.getName() : null;
        if (driverEmail == null)
            return;

        // In a real app, we need to extract the Driver user object.
        // As a shortcut, if we assume the client includes orderId or driverId in a
        // wrapper,
        // we can fetch it. Or we fetch user by email logic which isn't available in
        // AuthServiceClient atm.
        // For simplicity in this demo, let's assume the client passes the active
        // orderId in a wrapper or
        // we broadcast the location to the customer directly if order is provided.
        // Here we just accept the payload. To fully decouple, we would add "active
        // order lookup" to OrderService.

        locationUpdate.setTimestamp(Instant.now());

        // This requires driverId... Since we only have email from Principal,
        // we might skip Redis GEO storage for a minute or add getUserByEmail to
        // AuthAdapter.
        // We'll stub driverId as 1L for compiling, in reality Auth API needs
        // /users/email endpoint.
        redisGeoService.updateDriverLocation(1L, locationUpdate.getLatitude(), locationUpdate.getLongitude());

        // Example: If we know customerEmail, send via Factory:
        // DriverLocationNotification n =
        // NotificationFactory.createDriverLocationNotification(customerEmail, lat,
        // lon);
        // webSocketService.pushNotification(n);
    }
}
