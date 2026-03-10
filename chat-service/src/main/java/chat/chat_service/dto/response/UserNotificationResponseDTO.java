package chat.chat_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import chat.chat_service.model.NotificationType;

import java.time.Instant;

public record UserNotificationResponseDTO(
        @JsonProperty("type") NotificationType type,
        @JsonProperty("user_id") String userId,
        @JsonProperty("username") String username,
        @JsonProperty("room_id") String roomId,
        @JsonProperty("content") String content,
        @JsonProperty("timestamp") Instant timestamp
) {
}
