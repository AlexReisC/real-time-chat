package chat.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrivateMessageDTO(
        @NotBlank(message = "O ID do recipiente é obrigatório")
        String recipientId,

        @NotBlank(message = "O conteúdo não pode estar em branco")
        @Size(max = 1024, message = "A mensagem não pode exceder 1024 caracteres")
        String content
) {
}
