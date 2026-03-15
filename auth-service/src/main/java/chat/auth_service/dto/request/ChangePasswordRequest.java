package chat.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank 
        String currentPassword,
        
        @NotBlank 
        @Size(min = 8)
        @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+",
                message = "A senha deve ter no mínimo 8 caracteres com pelo menos uma letra maiúscula, um número e um caractere especial")
        String newPassword
) { }
