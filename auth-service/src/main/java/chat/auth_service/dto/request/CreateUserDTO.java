package chat.auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank(message = "O username é obrigatório")
        @Size(min = 3, max = 50, message = "O username deve conter entre 3 e 50 caracteres")
        String username,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve conter entre 8 e 72 caracteres")
        @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+",
                message = "A senha deve ter no mínimo 8 caracteres com pelo menos uma letra maiúscula, um número e um caractere especial")
        String password
) {
}
