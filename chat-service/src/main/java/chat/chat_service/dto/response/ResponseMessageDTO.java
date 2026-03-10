package chat.chat_service.dto.response;

import java.time.Instant;
import chat.chat_service.model.MessageType;

public record ResponseMessageDTO(
        String id,
        MessageType type,
        String roomId,
        String senderId,
        String recipientId,
        String content,
        Instant timestamp
) {
}
