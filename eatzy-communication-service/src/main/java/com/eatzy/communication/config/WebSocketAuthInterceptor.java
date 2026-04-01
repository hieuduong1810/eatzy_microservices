package com.eatzy.communication.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

/**
 * Interceptor to validate JWT tokens on initial WebSocket connection.
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthInterceptor(@Value("${foodDelivery.jwt.base64-secret}") String jwtKey) {
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                java.util.Base64.getDecoder().decode(jwtKey), "HmacSHA512");
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token != null && !token.isEmpty()) {
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    String email = jwt.getSubject();

                    if (email != null && !email.isEmpty()) {
                        attributes.put("email", email);
                        // Can also extract roles or userId if available in claims
                        log.info("✅ WebSocket authenticated for user: {}", email);
                        return true;
                    }
                } catch (Exception e) {
                    log.error("❌ WebSocket auth failed: {}", e.getMessage());
                }
            }
        }
        log.warn("❌ WebSocket connection rejected - no valid token provided in query params (?token=xxx)");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {}
}
