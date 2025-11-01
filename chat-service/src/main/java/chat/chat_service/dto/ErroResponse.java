package chat.chat_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
        String message,
        Integer status,
        LocalDateTime timestamp
) {
}
