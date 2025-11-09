package chat.auth_service.dto.response;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String email,
        String username
) {
}
