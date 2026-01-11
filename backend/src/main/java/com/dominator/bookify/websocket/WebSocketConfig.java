package com.dominator.bookify.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Kết nối từ React
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // cho phép mọi domain (dev)
                .withSockJS(); // hỗ trợ fallback nếu WS bị chặn
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // prefix nơi client nhận tin nhắn
        config.setApplicationDestinationPrefixes("/app"); // prefix nơi client gửi tin nhắn
    }
}
