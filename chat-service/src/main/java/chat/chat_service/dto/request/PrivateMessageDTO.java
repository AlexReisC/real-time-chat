package chat.chat_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrivateMessageDTO(
        @JsonProperty("recipientId")
        @NotBlank(message = "O ID do recipiente é obrigatório")
        String recipientId,

        @JsonProperty("content")
        @NotBlank(message = "O conteúdo não pode estar em branco")
        @Size(max = 1024, message = "A mensagem não pode exceder 1024 caracteres")
        String content
) {
}
