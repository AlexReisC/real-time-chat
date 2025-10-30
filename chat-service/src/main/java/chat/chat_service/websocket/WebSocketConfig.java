package chat.chat_service.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final UserHandshakeInterceptor userHandshakeInterceptor;

    public WebSocketConfig(UserHandshakeInterceptor userHandshakeInterceptor) {
        this.userHandshakeInterceptor = userHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app"); // for controller endpoint
        config.setUserDestinationPrefix("/user"); // private messages
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat") // endpoint for connection websocket
                .addInterceptors(userHandshakeInterceptor)
                .setAllowedOrigins("*") // allows cors
                .withSockJS();
    }
}
