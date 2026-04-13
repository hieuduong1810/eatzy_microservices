package com.eatzy.communication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP configuration for Microservices architecture.
 * 
 * Authentication is handled at the STOMP protocol level via
 * {@link WebSocketAuthChannelInterceptor} instead of HTTP handshake,
 * making it compatible with any transport (WebSocket, SockJS, etc.).
 * 
 * Flow: Client connects to /ws → sends STOMP CONNECT with JWT → gets authenticated.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    public WebSocketConfig(WebSocketAuthChannelInterceptor authChannelInterceptor) {
        this.authChannelInterceptor = authChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for /topic (broadcast) and /queue (point-to-point)
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for @MessageMapping endpoints
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific destinations (convertAndSendToUser)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Single STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register the STOMP-level authentication interceptor
        // This intercepts STOMP CONNECT frames to validate JWT and set Principal
        registration.interceptors(authChannelInterceptor);
    }
}
