package chat.chat_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import chat.chat_service.dto.request.PublicMessageDTO;
import chat.chat_service.dto.request.PrivateMessageDTO;
import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.dto.response.ResponseMessageDTO;
import chat.chat_service.model.Message;
import chat.chat_service.model.MessageType;
import chat.chat_service.repository.MessageRepository;
import io.lettuce.core.RedisException;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final RoomService roomService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_CACHED_MESSAGES = 50;
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    public MessageService(MessageRepository messageRepository, RoomService roomService, RedisTemplate<String, Object> redisTemplate) {
        this.messageRepository = messageRepository;
        this.roomService = roomService;
        this.redisTemplate = redisTemplate;
    }

    public ResponseMessageDTO savePublicMessage(PublicMessageDTO messageDTO, String senderId, String senderUsername){
        Message message = fromChatMessagetoEntity(messageDTO, senderId, senderUsername);
        message.setType(MessageType.ROOM);

        roomService.existById(message.getRoomId());

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

        cacheMessage(savedDto);

        return savedDto;
    }

    private void cacheMessage(ResponseMessageDTO message) {
        String cacheKey = "room:" + message.roomId() + ":messages";
        
        if (message.type() != MessageType.ROOM || message.roomId() == null) {
            String privateConversationKey = getPrivateConversationKey(message.senderId(), message.recipientId());
            cacheKey = cacheKey.replace("room:", privateConversationKey);
        }
        
        redisTemplate.opsForList().rightPush(cacheKey, message);
        redisTemplate.opsForList().trim(cacheKey, -MAX_CACHED_MESSAGES, -1);
        redisTemplate.expire(cacheKey, Duration.ofDays(1));
    }

    private String getPrivateConversationKey(String userId1, String userId2) {
        // Ordena os IDs para garantir que a chave é a mesma, independentemente de quem envia
        String userA = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        String userB = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        return "private:" + userA + ":" + userB + ":messages";
    }

    private Message fromChatMessagetoEntity(PublicMessageDTO messageDTO, String senderId, String senderUsername) {
        Message message = Message.builder()
                .type(MessageType.ROOM)
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
                .type(MessageType.PRIVATE)
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

        ResponseMessageDTO savedDto = new ResponseMessageDTO(
                savedMessage.getId(),
                savedMessage.getType(),
                savedMessage.getRoomId(),
                savedMessage.getSenderId(),
                savedMessage.getRecipientId(),
                savedMessage.getContent(),
                savedMessage.getTimestamp()
        );

        cacheMessage(savedDto);
        return savedDto;
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

    private boolean acquireLock(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(5)));
    }

    private void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }

    public List<ResponseMessageDTO> getRecentRoomMessages(String roomId) {
        String cacheKey = "room:" + roomId + ":messages";
        String lockKey = "lock:room:" + roomId;

        return fetchMessagesWithLock(cacheKey, lockKey, () -> 
            messageRepository.findTop50ByRoomIdOrderByTimestampDesc(roomId).stream()
                .limit(MAX_CACHED_MESSAGES)
                .map(this::toResponseDTO)
                .collect(Collectors.toList())
        );
    }

    public List<ResponseMessageDTO> getRecentPrivateMessages(String userId1, String userId2) {
        String privateConversationKey = getPrivateConversationKey(userId1, userId2);
        String lockKey = "lock:" + privateConversationKey;

        return fetchMessagesWithLock(privateConversationKey, lockKey, () -> 
            messageRepository.findTop50PrivateConversation(userId1, userId2).stream()
                .limit(MAX_CACHED_MESSAGES)
                .map(this::toResponseDTO)
                .collect(Collectors.toList())
        );
    }

    private List<ResponseMessageDTO> fetchMessagesWithLock(String cacheKey, String lockKey, Supplier<List<ResponseMessageDTO>> dbSupplier) {
        int retries = 3; // Tenta ler do cache 3 vezes antes de desistir
        
        while (retries > 0) {
            try {
                List<Object> cachedObjects = redisTemplate.opsForList().range(cacheKey, 0, -1);
                if (cachedObjects != null && !cachedObjects.isEmpty()) {
                    return cachedObjects.stream()
                            .map(obj -> {
                                if (obj instanceof ResponseMessageDTO dto) return dto;
                                if (obj instanceof Message msg) return toResponseDTO(msg);
                                return (ResponseMessageDTO) obj;
                            })
                            .collect(Collectors.toList());
                }
            } catch (RedisException e) {
                logger.warn("Redis indisponível, buscando do MongoDB: {}", e.getMessage());
                break;
            }

            // 2. CACHE MISS: TENTA ADQUIRIR O LOCK
            if (acquireLock(lockKey)) {
                try {
                    // Double-check: garante que outra thread não repovoou enquanto adquiríamos o lock
                    List<Object> doubleCheckCache = redisTemplate.opsForList().range(cacheKey, 0, -1);
                    if (doubleCheckCache != null && !doubleCheckCache.isEmpty()) {
                        continue; // Volta ao início do loop para ler o cache repovoado
                    }

                    // VENCEDORA: Busca do MongoDB
                    List<ResponseMessageDTO> messagesFromDb = dbSupplier.get();
                    Collections.reverse(messagesFromDb);

                    // Repovoa o cache
                    if (!messagesFromDb.isEmpty()) {
                        for (ResponseMessageDTO msg : messagesFromDb) {
                            redisTemplate.opsForList().rightPush(cacheKey, msg);
                        }
                        redisTemplate.expire(cacheKey, Duration.ofDays(1));
                    }
                    return messagesFromDb;

                } finally {
                    // SEMPRE liberta o lock no finally para evitar deadlocks
                    releaseLock(lockKey);
                }
            } else {
                // PERDEDORA: Outra thread está repovoando o cache. Espera 100ms e tenta novamente.
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                retries--;
            }
        }

        // FALLBACK DE SEGURANÇA (Se os retries acabarem ou o Redis cair)
        List<ResponseMessageDTO> fallbackMessages = dbSupplier.get();
        Collections.reverse(fallbackMessages);
        return fallbackMessages;
    }
}
