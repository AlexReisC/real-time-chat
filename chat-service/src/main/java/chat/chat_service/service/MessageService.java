package chat.chat_service.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import chat.chat_service.dto.request.PublicMessageDTO;
import chat.chat_service.dto.request.PrivateMessageDTO;
import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.dto.response.ResponseMessageDTO;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Message;
import chat.chat_service.model.MessageType;
import chat.chat_service.repository.MessageRepository;
import chat.chat_service.repository.RoomRepository;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_CACHED_MESSAGES = 50;

    public MessageService(MessageRepository messageRepository, RoomRepository roomRepository, RedisTemplate<String, Object> redisTemplate) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.redisTemplate = redisTemplate;
    }

    public ResponseMessageDTO savePublicMessage(PublicMessageDTO messageDTO, String senderId, String senderUsername){
        Message message = fromChatMessagetoEntity(messageDTO, senderId, senderUsername);
        message.setType(MessageType.ROOM);

        if (!roomRepository.existsById(message.getRoomId())) {
            throw new RoomNotFoundException("Sala não encontrada!");
        }

        Message savedMessage = messageRepository.save(message);
        ResponseMessageDTO savedDto = new ResponseMessageDTO(
                savedMessage.getId(),
                savedMessage.getType(),
                savedMessage.getRoomId(),
                savedMessage.getSenderId(),
                savedMessage.getRecipientId(),
                savedMessage.getContent(),
                savedMessage.getTimestamp()
        );
        
        String cacheKey = "room:" + message.getRoomId() + ":messages";
        redisTemplate.opsForList().rightPush(cacheKey, savedDto);
        redisTemplate.opsForList().trim(cacheKey, -MAX_CACHED_MESSAGES, -1);

        return savedDto;
    }

    private Message fromChatMessagetoEntity(PublicMessageDTO messageDTO, String senderId, String senderUsername) {
        Message message = Message.builder()
                .roomId(messageDTO.roomId())
                .content(messageDTO.content())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .recipientId(null)
                .timestamp(Instant.now())
                .build();
        return message;
    }

    private Message fromPrivateMessagetoEntity(PrivateMessageDTO messageDTO, String senderId, String senderUsername) {
        Message message = Message.builder()
                .roomId(null)
                .content(messageDTO.content())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .recipientId(messageDTO.recipientId())
                .timestamp(Instant.now())
                .build();
        return message;
    }

    private ResponseMessageDTO toResponseDTO(Message message) {
        return new ResponseMessageDTO(
                message.getId(),
                message.getType(),
                message.getRoomId(),
                message.getSenderId(),
                message.getRecipientId(),
                message.getContent(),
                message.getTimestamp()
        );
    }

    public ResponseMessageDTO savePrivateMessage(PrivateMessageDTO messageDTO, String senderId, String senderUsername) {
        Message message = fromPrivateMessagetoEntity(messageDTO, senderId, senderUsername);

        Message savedMessage = messageRepository.save(message);

        return new ResponseMessageDTO(
                savedMessage.getId(),
                savedMessage.getType(),
                savedMessage.getRoomId(),
                savedMessage.getSenderId(),
                savedMessage.getRecipientId(),
                savedMessage.getContent(),
                savedMessage.getTimestamp()
        );
    }

    public PageResponseDTO<ResponseMessageDTO> listAllMessages(String roomId, Pageable pageable) {
        Page<Message> messagePage = messageRepository.findByRoomId(roomId, pageable);

        List<ResponseMessageDTO> messageDTOList = messagePage.getContent().stream().map(this::toResponseDTO).toList();

        return new PageResponseDTO<>(
                messageDTOList,
                messagePage.getNumber(),
                messagePage.getTotalPages(),
                messagePage.getTotalElements(),
                messagePage.getSize()
        );
    }

    public PageResponseDTO<ResponseMessageDTO> listAllPrivateMessages(String senderId, String recipientId, Pageable pageable) {
        Page<Message> messagePage = messageRepository.findPrivateConversation(senderId, recipientId, pageable);

        List<ResponseMessageDTO> messageDTOList = messagePage.getContent().stream().map(this::toResponseDTO).toList();

        return new PageResponseDTO<>(
                messageDTOList,
                messagePage.getNumber(),
                messagePage.getTotalPages(),
                messagePage.getTotalElements(),
                messagePage.getSize()
        );
    }

    public List<ResponseMessageDTO> getRecentRoomMessages(String roomId) {
        String cacheKey = "room:" + roomId + ":messages";

        List<Object> cachedObjects = redisTemplate.opsForList().range(cacheKey, 0, -1);

        if (cachedObjects != null && !cachedObjects.isEmpty()) {
            return cachedObjects.stream()
                    .map(obj -> {
                        if (obj instanceof ResponseMessageDTO dto) {
                            return dto;
                        }
                        if (obj instanceof Message msg) {
                            return toResponseDTO(msg);
                        }
                        return (ResponseMessageDTO) obj;
                    })
                    .collect(Collectors.toList());
        }

        List<ResponseMessageDTO> messagesFromDb = messageRepository.findTop50ByRoomIdOrderByTimestampDesc(roomId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        Collections.reverse(messagesFromDb);

        if (!messagesFromDb.isEmpty()) {
            for (ResponseMessageDTO msg : messagesFromDb) {
                redisTemplate.opsForList().rightPush(cacheKey, msg);
            }
        }

        return messagesFromDb;
    }
}
