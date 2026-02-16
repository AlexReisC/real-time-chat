package chat.chat_service.controller;

import java.time.Instant;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestMapping;

import chat.chat_service.dto.request.ChatMessageDTO;
import chat.chat_service.dto.request.PrivateMessageDTO;
import chat.chat_service.dto.request.UserNotificationDTO;
import chat.chat_service.dto.response.ErroResponse;
import chat.chat_service.dto.response.UserNotificationResponseDTO;
import chat.chat_service.model.Message;
import chat.chat_service.service.MessageService;
import chat.chat_service.service.RoomService;
import jakarta.validation.Valid;

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
    public void addUser(@Valid @Payload UserNotificationDTO notificationDTO,
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

        UserNotificationResponseDTO response = new UserNotificationResponseDTO(
                "JOIN",
                userId,
                username,
                roomId,
                String.format("%s entrou na sala!", username),
                Instant.now()
        );

        messagingTemplate.convertAndSend("/topic/rooms." + roomId, response);
    }

    @MessageMapping("/chat.removeUser")
    public void removeUser(@Valid @Payload UserNotificationDTO notificationDTO,
                                                  SimpMessageHeaderAccessor headerAccessor) {

        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");
        String roomId = notificationDTO.roomId();

        roomService.removeUser(roomId, senderUsername);
        logger.info("Usuário {} ({}) saiu da sala {}", senderUsername, senderId, roomId);
        
        UserNotificationResponseDTO response = new UserNotificationResponseDTO(
                "LEAVE",
                senderId,
                senderUsername,
                roomId,
                senderUsername + " saiu da sala",
                Instant.now()
        );

        messagingTemplate.convertAndSend("/topic/rooms." + roomId, response);
    }

    @MessageMapping("/chat.sendPublic")
    public void sendPublicMessage(@Valid @Payload ChatMessageDTO chatMessageDTO, SimpMessageHeaderAccessor headerAccessor){
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");

        logger.info("Mensagem recebida de {} na sala {}: {}", senderUsername, chatMessageDTO.roomId(), chatMessageDTO.content());

        Message savedMessage = messageService.savePublicMessage(chatMessageDTO, senderId, senderUsername);
        messagingTemplate.convertAndSend("/topic/rooms." + chatMessageDTO.roomId(), savedMessage);
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
