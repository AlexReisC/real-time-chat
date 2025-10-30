package chat.chat_service.controller;

import chat.chat_service.dto.ChatMessageDTO;
import chat.chat_service.dto.UserNotificationDTO;
import chat.chat_service.dto.UserNotificationResponseDTO;
import chat.chat_service.service.MessageService;
import chat.chat_service.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;

@Controller
@RequestMapping("/ws/chat")
public class ChatController {
    private final MessageService messageService;
    private final RoomService roomService;

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(MessageService messageService, SimpMessagingTemplate messagingTemplate, RoomService roomService) {
        this.messageService = messageService;
        this.roomService = roomService;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/{roomId}")
    public UserNotificationResponseDTO addUser(@Payload UserNotificationDTO notificationDTO, SimpMessageHeaderAccessor headerAccessor){
        if (headerAccessor == null){
            throw new IllegalStateException("Atributos de sessão estão null ao adicionar usuário");
        }

        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        String roomId = notificationDTO.roomId();
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        roomService.addNewUser(roomId, username);
        logger.info("Usuário {} ({}) entrou na sala {}", username, userId, roomId);

        return new UserNotificationResponseDTO(
                "JOIN",
                userId,
                username,
                roomId,
                String.format("%s entrou na sala!", username),
                Instant.now()
        );
    }
}
