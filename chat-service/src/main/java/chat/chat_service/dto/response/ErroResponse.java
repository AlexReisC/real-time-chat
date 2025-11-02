package chat.chat_service.dto.response;

import java.time.LocalDateTime;

public record ErroResponse(
        String message,
        Integer status,
        LocalDateTime timestamp
) {
}
