package chat.chat_service.dto.response;

import java.time.Instant;

public record ResponseMessageDTO(
        String id,
        String roomId,
        String senderId,
        String recipient,
        String content,
        Instant timestamp
) {
}
