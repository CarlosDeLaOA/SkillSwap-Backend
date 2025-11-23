package com.project.skillswap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-documents")
                .setAllowedOrigins("http://localhost:4200", "http://localhost:8080")
                .withSockJS();

        registry.addEndpoint("/ws-documents")
                .setAllowedOrigins("http://localhost:4200", "http://localhost:8080");

        System.out.println("========================================");
        System.out.println("   WebSocket endpoints registrados");
        System.out.println("   Allowed origins: localhost:4200, localhost:8080");
        System.out.println("========================================");
    }
}
