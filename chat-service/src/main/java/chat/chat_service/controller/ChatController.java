package chat.chat_service.controller;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import chat.chat_service.dto.request.PrivateMessageDTO;
import chat.chat_service.dto.request.PublicMessageDTO;
import chat.chat_service.dto.request.UserNotificationDTO;
import chat.chat_service.dto.response.ErrorResponse;
import chat.chat_service.dto.response.ResponseMessageDTO;
import chat.chat_service.dto.response.UserNotificationResponseDTO;
import chat.chat_service.model.NotificationType;
import chat.chat_service.service.MessageService;
import chat.chat_service.service.RoomService;
import jakarta.validation.Valid;

@Controller
public class ChatController {
    private final MessageService messageService;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(MessageService messageService, SimpMessagingTemplate messagingTemplate, RoomService roomService, StringRedisTemplate redisTemplate) {
        this.messageService = messageService;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Valid @Payload UserNotificationDTO notificationDTO, SimpMessageHeaderAccessor headerAccessor){
        if (headerAccessor == null){
            throw new IllegalStateException("Atributos de sessão estão null ao adicionar usuário");
        }

        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String redisKey = "user:" + userId + ":username";
        String username = redisTemplate.opsForValue().get(redisKey);

        String roomId = notificationDTO.roomId();
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        logger.info("Usuário {} ({}) entrou na sala {}", username, userId, roomId);

        UserNotificationResponseDTO response = new UserNotificationResponseDTO(
                NotificationType.JOIN,
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
        String redisKey = "user:" + senderId + ":username";
        String senderUsername = redisTemplate.opsForValue().get(redisKey);
        
        String roomId = notificationDTO.roomId();

        roomService.removeUser(roomId, senderId);
        logger.info("Usuário {} ({}) saiu da sala {}", senderUsername, senderId, roomId);
        
        UserNotificationResponseDTO response = new UserNotificationResponseDTO(
                NotificationType.LEAVE,
                senderId,
                senderUsername,
                roomId,
                senderUsername + " saiu da sala",
                Instant.now()
        );

        messagingTemplate.convertAndSend("/topic/rooms." + roomId, response);
    }

    @MessageMapping("/chat.sendPublic")
    public void sendPublicMessage(@Valid @Payload PublicMessageDTO chatMessageDTO, SimpMessageHeaderAccessor headerAccessor){
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        String redisKey = "user:" + senderId + ":username";
        String senderUsername = redisTemplate.opsForValue().get(redisKey);

        logger.info("Mensagem recebida de {} na sala {}", senderUsername, chatMessageDTO.roomId());

        ResponseMessageDTO savedMessage = messageService.savePublicMessage(chatMessageDTO, senderId, senderUsername);
        messagingTemplate.convertAndSend("/topic/rooms." + chatMessageDTO.roomId(), savedMessage);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleGenericException(Exception exception) {
        logger.error("Erro não tratado no WebSocket: {}", exception.getMessage(), exception);
        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "Ocorreu um erro inesperado";

        return new ErrorResponse(
                errorMessage,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now()
        );
    }

    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Valid @Payload PrivateMessageDTO messageDTO, SimpMessageHeaderAccessor headerAccessor) {
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        String redisKey = "user:" + senderId + ":username";
        String senderUsername = redisTemplate.opsForValue().get(redisKey);

        logger.info("Mensagem enviada de {} para {}", senderUsername, messageDTO.recipientId());

        ResponseMessageDTO saved = messageService.savePrivateMessage(messageDTO, senderId, senderUsername);

        messagingTemplate.convertAndSendToUser(saved.recipientId(), "/queue/private", saved);

        messagingTemplate.convertAndSendToUser(senderId, "/queue/private", saved);
    }
}
