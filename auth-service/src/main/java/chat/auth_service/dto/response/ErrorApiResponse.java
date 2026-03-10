package chat.auth_service.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorApiResponse(
        String message,
        List<String> errors,
        Instant timestamp
) {
}
