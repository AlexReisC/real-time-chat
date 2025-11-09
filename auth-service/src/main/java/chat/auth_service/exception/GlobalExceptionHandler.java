package chat.auth_service.exception;

import chat.auth_service.dto.response.ErrorApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorApiResponse> handleUserNotFound(UsernameNotFoundException exception){
        ErrorApiResponse errorApiResponse = new ErrorApiResponse(
                "Usuário não encontrado",
                List.of(exception.getMessage()),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorApiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> erros = new ArrayList<>();
        exception.getBindingResult()
                .getFieldErrors().forEach(er -> erros.add(er.getField() + ": " + er.getDefaultMessage()));

        ErrorApiResponse erroApiResponse = new ErrorApiResponse(
                "Dados de entrada inválidos",
                erros,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(erroApiResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorApiResponse> handleBadCredentialsException(BadCredentialsException exception) {
        List<String> erros = new ArrayList<>();
        erros.add(exception.getMessage());

        ErrorApiResponse erroApiResponse = new ErrorApiResponse(
                "Dados de entrada inválidos",
                erros,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(erroApiResponse);
    }
}
