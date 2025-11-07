package chat.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginUserDTO(
        @NotBlank(message = "O email é obrigatório")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
