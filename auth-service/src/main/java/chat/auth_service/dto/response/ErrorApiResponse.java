package chat.auth_service.dto.response;

import java.time.Instant;

import org.springframework.http.HttpStatus;

public record ErrorApiResponse(
		Instant timestamp,
        int status,
        String error,
        String message
) {
	public static ErrorApiResponse of(HttpStatus status, String message) {
		return new ErrorApiResponse(
			Instant.now(),
			status.value(),
			status.getReasonPhrase(),
			message
		);
	}
}
