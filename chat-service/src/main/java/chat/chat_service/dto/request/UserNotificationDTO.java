package chat.chat_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import chat.chat_service.model.NotificationType;
import jakarta.validation.constraints.NotBlank;

public record UserNotificationDTO(
        @NotBlank(message = "O ID da sala é obrigatório")
        @JsonProperty("room_id") String roomId,

        @NotBlank(message = "O tipo de notificação é obrigatório")
        @JsonProperty("type") NotificationType type
) {
}
