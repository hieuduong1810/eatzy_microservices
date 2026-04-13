package com.eatzy.communication.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * STOMP-level authentication interceptor for WebSocket connections.
 * 
 * Intercepts the STOMP CONNECT frame to validate the JWT token
 * and set the authenticated Principal for the WebSocket session.
 * 
 * This approach is the microservices best practice because:
 * 1. Works with any transport (WebSocket, SockJS, polling)
 * 2. Does not depend on Servlet API (compatible with WebFlux gateway)
 * 3. Authentication happens at protocol level, not HTTP level
 * 
 * Client must send token in one of these ways:
 * - STOMP header: Authorization: Bearer {token}
 * - STOMP native header: token: {token}
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthChannelInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from STOMP headers
            String token = extractToken(accessor);

            if (token == null || token.isEmpty()) {
                log.warn("❌ WebSocket CONNECT rejected - no token provided");
                throw new MessagingException("Authentication required. Provide JWT token in Authorization header or 'token' native header.");
            }

            try {
                Jwt jwt = jwtDecoder.decode(token);
                String email = jwt.getSubject();

                if (email == null || email.isEmpty()) {
                    throw new MessagingException("Invalid JWT: no subject (email) found");
                }

                // Extract authorities/roles from JWT claims
                List<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);

                // Create authentication token and set as user Principal
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                accessor.setUser(authentication);
                log.info("✅ WebSocket authenticated for user: {} with {} authorities", email, authorities.size());

            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                log.error("❌ WebSocket auth failed: {}", e.getMessage());
                throw new MessagingException("Authentication failed: " + e.getMessage());
            }
        }

        return message;
    }

    /**
     * Extract JWT token from STOMP headers.
     * Supports two formats:
     * 1. Authorization: Bearer {token}
     * 2. token: {token} (native header)
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Try "Authorization: Bearer xxx" header first
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fallback: try "token" native header
        String token = accessor.getFirstNativeHeader("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        return null;
    }

    /**
     * Extract granted authorities from JWT claims.
     * Looks for "permission" claim (matching SecurityConfig's JwtGrantedAuthoritiesConverter).
     */
    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        Object permissionClaim = jwt.getClaim("permission");
        if (permissionClaim instanceof List) {
            return ((List<String>) permissionClaim).stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
