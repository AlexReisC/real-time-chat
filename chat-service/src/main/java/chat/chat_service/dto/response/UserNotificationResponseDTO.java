package chat.chat_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UserNotificationResponseDTO(
        @JsonProperty("type") String type,  // "JOIN" or "LEAVE"
        @JsonProperty("userId") String userId,
        @JsonProperty("username") String username,
        @JsonProperty("roomId") String roomId,
        @JsonProperty("content") String content,
        @JsonProperty("timestamp") Instant timestamp
) {
}
