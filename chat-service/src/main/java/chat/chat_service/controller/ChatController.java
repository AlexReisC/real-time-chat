package chat.chat_service.controller;

import chat.chat_service.dto.ChatMessageDTO;
import chat.chat_service.service.MessageService;
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
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/{roomId}")
    public ChatMessageDTO addUser(@Payload ChatMessageDTO chatMessage, SimpMessageHeaderAccessor headerAccessor){
        if (headerAccessor == null){
            throw new IllegalStateException("Atributos de sessão estão null ao adicionar usuário");
        }

        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        String roomId = chatMessage.roomId();
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        logger.info("Usuário {} ({}) entrou na sala {}", username, userId, roomId);

        return new ChatMessageDTO(
                "JOIN",
                roomId,
                username,
                String.format("%s entrou na sala!", username),
                Instant.now()
        );
    }
}
