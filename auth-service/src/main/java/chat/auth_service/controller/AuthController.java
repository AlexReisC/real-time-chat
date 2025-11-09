package chat.auth_service.controller;

import chat.auth_service.dto.request.CreateUserDTO;
import chat.auth_service.dto.request.LoginUserDTO;
import chat.auth_service.dto.response.RecoveryTokenDTO;
import chat.auth_service.dto.response.UserResponseDTO;
import chat.auth_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<RecoveryTokenDTO> login(@Valid @RequestBody LoginUserDTO loginUserDTO) {
        RecoveryTokenDTO tokenDTO = authService.authenticateUser(loginUserDTO);
        return ResponseEntity.ok(tokenDTO);
    }

}
