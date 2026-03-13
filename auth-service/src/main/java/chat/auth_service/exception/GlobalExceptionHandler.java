package chat.auth_service.exception;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import chat.auth_service.dto.response.ErrorApiResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.InvalidKeyException;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .sorted()
                        .toList();
        
        return ResponseEntity
                .badRequest()
                .body(ErrorApiResponse.of(HttpStatus.BAD_REQUEST, errors));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorApiResponse> handleBadCredentialsException(BadCredentialsException exception) {
        var errorApiResponse = ErrorApiResponse.of(HttpStatus.UNAUTHORIZED, List.of("Credenciais inválidas"));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorApiResponse);
    }

    @ExceptionHandler({JwtException.class, InvalidKeyException.class})
    public ResponseEntity<ErrorApiResponse> handleJwtExceptions(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApiResponse.of(HttpStatus.UNAUTHORIZED, List.of("Token inválido ou expirado")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApiResponse> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorApiResponse.of(HttpStatus.FORBIDDEN, List.of("Acesso negado")));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorApiResponse> handleEmailExisting(EmailAlreadyExistsException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorApiResponse.of(HttpStatus.CONFLICT, List.of("Email já está em uso")));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorApiResponse> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorApiResponse.of(HttpStatus.NOT_FOUND, List.of(exception.getMessage())));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorApiResponse> handleInvalidPassword(InvalidPasswordException exception) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorApiResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, List.of(exception.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApiResponse> handleGeneric(Exception ex) {
        logger.error("Erro interno não tratado", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorApiResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                        List.of("Erro interno. Tente novamente mais tarde.")));
    }
}
