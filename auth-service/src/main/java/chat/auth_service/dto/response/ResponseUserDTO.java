package chat.auth_service.dto.response;

import java.util.UUID;

public record ResponseUserDTO(
        UUID id,
        String email,
        String username
) {
}
