package chat.chat_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import chat.chat_service.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserNotificationDTO(
        @NotBlank(message = "O ID da sala é obrigatório")
        String roomId,

        @NotNull(message = "O tipo de notificação é obrigatório")
        NotificationType type
) {
}
