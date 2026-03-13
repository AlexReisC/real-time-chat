package chat.auth_service.dto.response;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

public record ErrorApiResponse(
		Instant timestamp,
        int status,
        String message,
        List<String> errors
) {
	public static ErrorApiResponse of(HttpStatus status, List<String> errors) {
		return new ErrorApiResponse(
			Instant.now(),
			status.value(),
			status.getReasonPhrase(),
			errors
		);
	}
}
