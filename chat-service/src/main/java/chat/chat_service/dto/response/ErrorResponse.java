package chat.chat_service.dto.response;

import java.time.Instant;

public record ErrorResponse(
        String message,
        Integer status,
        Instant timestamp
) {
}
