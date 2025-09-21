package com.example.shoozy_shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký endpoint cho WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:5173/")
                .withSockJS(); // Sử dụng SockJS để hỗ trợ các trình duyệt không hỗ trợ WebSocket
        log.info("WebSocket endpoint registered at /ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Cấu hình message broker
        registry.enableSimpleBroker("/topic","/queue"); // Sử dụng một message broker đơn giản
        registry.setApplicationDestinationPrefixes("/app"); // Tiền tố cho các điểm đến của ứng dụng
        log.info("Message broker configured with prefixes /topic and /queue");
    }
}