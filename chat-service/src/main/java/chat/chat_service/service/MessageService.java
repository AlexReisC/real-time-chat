package chat.chat_service.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public MessageService(MessageRepository messageRepository, RoomRepository roomRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
    }

    public ResponseMessageDTO savePublicMessage(PublicMessageDTO messageDTO, String senderId, String senderUsername){
        Message message = fromChatMessagetoEntity(messageDTO, senderId, senderUsername);
        message.setType(MessageType.ROOM);

        if (!roomRepository.existsById(message.getRoomId())) {
            throw new RoomNotFoundException("Sala não encontrada!");
        }

        return messageRepository.save(message);
    }

    private Message toEntity(ChatMessageDTO messageDTO, String senderId, String senderUsername) {
        Message message = new Message();
        message.setRoomId(messageDTO.roomId());
        message.setContent(messageDTO.content());
        message.setSenderId(senderId);
        message.setSenderUsername(senderUsername);
        message.setRecipientId(messageDTO.recipientId());
        message.setTimestamp(Instant.now());
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
        Message message = Message.builder()
                .senderId(senderId)
                .senderUsername(senderUsername)
                .recipientId(messageDTO.recipientId())
                .content(messageDTO.content())
                .timestamp(Instant.now())
                .roomId(null)
                .type(MessageType.PRIVATE)
                .build();

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
}
