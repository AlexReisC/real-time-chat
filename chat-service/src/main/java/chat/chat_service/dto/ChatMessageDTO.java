package chat.chat_service.dto;

import java.time.Instant;

public record ChatMessageDTO(
        String type,
        String roomId,
        String sender,
        String content,
        Instant timestamp
) {
}
