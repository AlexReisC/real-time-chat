package chat.chat_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserNotificationDTO(
        @JsonProperty("roomId") String roomId,
        @JsonProperty("type") String type
) {
}
