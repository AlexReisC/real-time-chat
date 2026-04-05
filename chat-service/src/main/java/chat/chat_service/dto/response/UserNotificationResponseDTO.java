package chat.chat_service.dto.response;

import chat.chat_service.model.NotificationType;

import java.time.Instant;

public record UserNotificationResponseDTO(
        NotificationType type,
        String userId,
        String username,
        String roomId,
        String content,
        Instant timestamp
) {
}
