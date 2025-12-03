package com.project.skillswap.config;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración general para WebSockets en SkillSwap.
 * Define brokers, endpoints STOMP y reglas de conexión.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    //#region Message Broker Configuration

    /**
     * Configura el message broker interno y los prefijos utilizados
     * por los clientes para enviar y recibir mensajes.
     *
     * @param registry Registro del broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");      // Prefijo de mensajes emitidos
        registry.setApplicationDestinationPrefixes("/app"); // Prefijo de mensajes entrantes
    }

    //#endregion



    //#region STOMP Endpoints Registration

    /**
     * Registra los endpoints WebSocket/STOMP disponibles para los clientes.
     * Incluye soporte SockJS para fallback.
     *
     * @param registry Registro de endpoints STOMP
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws-documents")
                .setAllowedOrigins("http://localhost:4200", "http://localhost:8080")
                .withSockJS();

        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    //#endregion
}
