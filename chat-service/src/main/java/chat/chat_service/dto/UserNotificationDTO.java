package chat.chat_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record UserNotificationDTO(
        @NotBlank(message = "O ID da sala é obrigatório")
        @JsonProperty("roomId") String roomId,

        @NotBlank(message = "O tipo de notificação é obrigatório")
        @JsonProperty("type") String type
) {
}
