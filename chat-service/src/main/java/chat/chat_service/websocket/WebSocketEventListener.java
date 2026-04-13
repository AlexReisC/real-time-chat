package chat.chat_service.websocket;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import chat.chat_service.dto.response.UserNotificationResponseDTO;
import chat.chat_service.model.NotificationType;

@Component
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String userId = sessionAttributes != null ? (String) sessionAttributes.get("userId") : null;
        String username = sessionAttributes != null ? (String) sessionAttributes.get("username") : null;
        String sessionId = headerAccessor.getSessionId();
        String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userId != null || principalName != null) {
            logger.info("Sessão websocket conectada: sessionId={}, userId={}, username={}, principal={}",
                    sessionId, userId, username, principalName);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if (userId != null && roomId != null) {
            UserNotificationResponseDTO response = new UserNotificationResponseDTO(
                    NotificationType.LEAVE, userId, username, roomId, username + " saiu da sala (desconectado)", Instant.now()
            );
            messagingTemplate.convertAndSend("/topic/rooms." + roomId, response);
        }
    }
}
