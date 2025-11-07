package chat.auth_service.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorApiResponse(
        String message,
        List<String> errors,
        Integer statusCode,
        LocalDateTime timestamp
) {
}
