package chat.chat_service.service;

import chat.chat_service.dto.ChatMessageDTO;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Message;
import chat.chat_service.repository.MessageRepository;
import chat.chat_service.repository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;

    public MessageService(MessageRepository messageRepository, RoomRepository roomRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
    }

    public Message saveMessage(ChatMessageDTO messageDTO, String senderId, String senderUsername){
        Message message = messageDTO.toEntity(senderId, senderUsername);

        if (!roomRepository.existsById(message.getRoomId())) {
            throw new RoomNotFoundException("Sala não encontrada!");
        }

        return messageRepository.save(message);
    }

}
