package com.example.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // ✅ FIX: Dùng allowedOriginPatterns thay vì "*" để tương thích với allowCredentials
                .setAllowedOriginPatterns("http://localhost:*", "https://*.yourdomain.com")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class
                );

                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");

                    if (authHeaders == null || authHeaders.isEmpty()) {
                        log.warn("WebSocket CONNECT without Authorization header — rejected");
                        throw new IllegalArgumentException("Missing Authorization header");
                    }

                    String bearerToken = authHeaders.get(0);
                    if (!bearerToken.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("Invalid Authorization format");
                    }

                    String tokenValue = bearerToken.substring(7);

                    try {
                        Jwt jwt = jwtDecoder.decode(tokenValue);

                        String email = jwt.getClaimAsString("email");
                        String name = (email != null) ? email : jwt.getSubject();


                        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(), name);
                        accessor.setUser(auth);

                        log.info("WebSocket authenticated for user: {}", name);

                    } catch (Exception e) {
                        log.warn("WebSocket JWT validation failed: {}", e.getMessage());
                        throw new IllegalArgumentException("Invalid or expired token");
                    }
                }

                return message;
            }
        });
    }
}