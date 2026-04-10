package chat.chat_service.websocket;

import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import chat.chat_service.dto.response.UserNotificationResponseDTO;
import chat.chat_service.model.NotificationType;

@Component
public class WebSocketEventListener {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if (userId != null) {
            UserNotificationResponseDTO response = new UserNotificationResponseDTO(
                    NotificationType.LEAVE, userId, username, roomId, username + " saiu da sala (desconectado)", Instant.now()
            );
            messagingTemplate.convertAndSend("/topic/rooms." + roomId, response);
        }
    }
}
