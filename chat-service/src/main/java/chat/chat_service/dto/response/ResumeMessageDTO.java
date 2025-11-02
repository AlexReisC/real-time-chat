package chat.chat_service.dto.response;

import java.time.Instant;

public record ResumeMessageDTO(
        String id,
        String roomId,
        String senderId,
        String content,
        Instant timestamp
) {
}
