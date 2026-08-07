package com.dominator.gearly.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsConfig corsConfig;

    public WebSocketConfig(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // React client connects here; allowed origins come from the shared
        // cors.allowed-origins property (no more wildcard).
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns(corsConfig.getAllowedOrigins().toArray(new String[0]))
                .withSockJS(); // SockJS fallback when raw WebSocket is blocked
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // prefix clients subscribe to
        config.setApplicationDestinationPrefixes("/app"); // prefix clients send to
    }
}
