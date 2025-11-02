package chat.chat_service.controller;

import chat.chat_service.dto.*;
import chat.chat_service.model.Message;
import chat.chat_service.service.MessageService;
import chat.chat_service.service.RoomService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/ws/chat")
public class ChatController {
    private final MessageService messageService;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(MessageService messageService, SimpMessagingTemplate messagingTemplate, RoomService roomService) {
        this.messageService = messageService;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/{roomId}")
    public UserNotificationResponseDTO addUser(@Valid @Payload UserNotificationDTO notificationDTO,
                                               SimpMessageHeaderAccessor headerAccessor){
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

    @MessageMapping("/chat.removeUser")
    @SendTo("/topic/{roomId}")
    public UserNotificationResponseDTO removeUser(@Valid @Payload UserNotificationDTO notificationDTO,
                                                  SimpMessageHeaderAccessor headerAccessor) {
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");

        roomService.removeUser(notificationDTO.roomId(), senderUsername);
        logger.info("Usuário {} ({}) saiu da sala {}", senderUsername, senderId, notificationDTO.roomId());
        return new UserNotificationResponseDTO(
                "LEAVE",
                senderId,
                senderUsername,
                notificationDTO.roomId(),
                senderUsername + " saiu da sala",
                Instant.now()
        );
    }

    @MessageMapping("/chat.sendPublic")
    @SendTo("/topic/{roomId}")
    public Message sendPublicMessage(@Valid @Payload ChatMessageDTO chatMessageDTO, SimpMessageHeaderAccessor headerAccessor){
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");

        logger.info("Mensagem recebida de {} na sala {}: {}", senderUsername, chatMessageDTO.roomId(), chatMessageDTO.content());

        return messageService.savePublicMessage(chatMessageDTO, senderId, senderUsername);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErroResponse handleValidationException(MethodArgumentNotValidException exception) {
        String errorMessage = exception.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();

        ErroResponse erroResponse = new ErroResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );

        logger.warn("Erro de validação ao processar mensagem: {}", errorMessage);

        return erroResponse;
    }

    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Valid @Payload PrivateMessageDTO messageDTO, SimpMessageHeaderAccessor headerAccessor) {
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");

        logger.info("Mensagem enviada de {} para {}", senderUsername, messageDTO.recipientId());

        Message saved = messageService.savePrivateMessage(messageDTO, senderId, senderUsername);

        messagingTemplate.convertAndSendToUser(saved.getRecipientId(), "/queue/private", saved);

        messagingTemplate.convertAndSendToUser(senderId, "/queue/private", saved);
    }
}
