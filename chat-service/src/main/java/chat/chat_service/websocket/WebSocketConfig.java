package chat.chat_service.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${chat.websocket.broker-type:relay}")
    private String brokerType;

    @Value("${relay.host}")
    private String relayHost;

    @Value("${relay.port}")
    private int relayPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUser;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPass;

    private final UserHandshakeInterceptor userHandshakeInterceptor;
    private final UserHandshakeHandler userHandshakeHandler;

    public WebSocketConfig(UserHandshakeInterceptor userHandshakeInterceptor, UserHandshakeHandler userHandshakeHandler) {
        this.userHandshakeInterceptor = userHandshakeInterceptor;
        this.userHandshakeHandler = userHandshakeHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if ("simple".equalsIgnoreCase(brokerType)) {
            config.enableSimpleBroker("/topic", "/queue");
        } else {
            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(rabbitUser)
                    .setClientPasscode(rabbitPass)
                    .setSystemLogin(rabbitUser)
                    .setSystemPasscode(rabbitPass);
        }
        
        config.setApplicationDestinationPrefixes("/app"); // for controller endpoint
        config.setUserDestinationPrefix("/user"); // private messages
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(userHandshakeHandler)
                .addInterceptors(userHandshakeInterceptor);
    }
}
