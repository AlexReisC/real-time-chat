package chat.auth_service.exception;

import java.security.InvalidKeyException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import chat.auth_service.dto.response.ErrorApiResponse;
import io.jsonwebtoken.JwtException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorApiResponse> handleUserNotFound(UsernameNotFoundException exception) {
        var errorApiResponse = ErrorApiResponse.of(HttpStatus.NOT_FOUND, "Usuário não encontrado");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorApiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiResponse> handleValidationException(MethodArgumentNotValidException exception) {
        
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        
        return ResponseEntity
                .badRequest()
                .body(ErrorApiResponse.of(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorApiResponse> handleBadCredentialsException(BadCredentialsException exception) {
        var errorApiResponse = ErrorApiResponse.of(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorApiResponse);
    }

    @ExceptionHandler({JwtException.class, InvalidKeyException.class})
    public ResponseEntity<ErrorApiResponse> handleJwt(JwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApiResponse.of(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApiResponse> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorApiResponse.of(HttpStatus.FORBIDDEN, "Acesso negado"));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorApiResponse> handleEmailExisting(EmailAlreadyExistsException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorApiResponse.of(HttpStatus.CONFLICT, "Email já está em uso"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorApiResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ErrorApiResponse.of(
                        HttpStatus.resolve(ex.getStatusCode().value()),
                        ex.getReason()));
    }
}
