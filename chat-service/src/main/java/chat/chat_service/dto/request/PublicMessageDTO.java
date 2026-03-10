package chat.chat_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicMessageDTO(
        @NotBlank(message = "O ID da sala é obrigatório")
        @JsonProperty("room_id") String roomId,

        @NotBlank(message = "O conteúdo não pode estar em branco")
        @Size(max = 1024, message = "A mensagem não pode exceder 1024 caracteres")
        @JsonProperty("content") String content
) {
    
}
