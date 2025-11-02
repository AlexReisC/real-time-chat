package chat.chat_service.service;

import chat.chat_service.dto.request.ChatMessageDTO;
import chat.chat_service.dto.request.PrivateMessageDTO;
import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.dto.response.ResponseMessageDTO;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Message;
import chat.chat_service.repository.MessageRepository;
import chat.chat_service.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;

    public MessageService(MessageRepository messageRepository, RoomRepository roomRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
    }

    public Message savePublicMessage(ChatMessageDTO messageDTO, String senderId, String senderUsername){
        Message message = messageDTO.toEntity(senderId, senderUsername);

        if (!roomRepository.existsById(message.getRoomId())) {
            throw new RoomNotFoundException("Sala não encontrada!");
        }

        return messageRepository.save(message);
    }

    public Message savePrivateMessage(PrivateMessageDTO messageDTO, String senderId, String senderUsername) {
        Message message = Message.builder()
                .senderId(senderId)
                .senderUsername(senderUsername)
                .recipientId(messageDTO.recipientId())
                .content(messageDTO.content())
                .timestamp(Instant.now())
                .roomId(null)
                .build();

        return messageRepository.save(message);
    }

    public PageResponseDTO<Message> listAllMessages(String roomId, Pageable pageable) {
        Page<Message> messagePage = messageRepository.findByRoomId(roomId, pageable);

        List<Message> messageDTOList = messagePage.getContent().stream().toList();

        return new PageResponseDTO<>(
                messageDTOList,
                messagePage.getNumber(),
                messagePage.getTotalPages(),
                messagePage.getTotalElements(),
                messagePage.getSize()
        );
    }

    public PageResponseDTO<Message> listAllPrivateMessages(String senderId, String recipientId, Pageable pageable) {
        Page<Message> messagePage = messageRepository.findPrivateConversation(senderId, recipientId, pageable);

        List<Message> messageDTOList = messagePage.getContent().stream().toList();

        return new PageResponseDTO<>(
                messageDTOList,
                messagePage.getNumber(),
                messagePage.getTotalPages(),
                messagePage.getTotalElements(),
                messagePage.getSize()
        );
    }
}
