package chat.auth_service.exception;

import chat.auth_service.dto.response.ErrorApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorApiResponse> handleClientAlreadyExists(UsernameNotFoundException exception){
        ErrorApiResponse errorApiResponse = new ErrorApiResponse(
                "Usuário não encontrado",
                List.of(exception.getMessage()),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorApiResponse);
    }


}
