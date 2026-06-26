package com.ycyw.chat.config;

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
        // Endpoint WebSocket natif (clients compatibles)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Endpoint SockJS : fallback HTTP long-polling pour les environnements qui bloquent WebSocket
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Les messages préfixés /app sont routés vers les @MessageMapping du serveur
        config.setApplicationDestinationPrefixes("/app");
        // Broker en mémoire : suffisant pour un POC, à remplacer par RabbitMQ/Redis en prod
        config.enableSimpleBroker("/topic");
    }
}
