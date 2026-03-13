package chat.auth_service.controller;

import chat.auth_service.dto.request.CreateUserDTO;
import chat.auth_service.dto.request.LoginUserDTO;
import chat.auth_service.dto.request.RefreshRequest;
import chat.auth_service.dto.response.AuthTokenDTO;
import chat.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthTokenDTO> register(@Valid @RequestBody CreateUserDTO createUserDTO) {
        AuthTokenDTO responseDTO = authService.createUser(createUserDTO);
        return ResponseEntity.created(URI.create("api/v1/auth/register")).body(responseDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenDTO> login(@Valid @RequestBody LoginUserDTO loginUserDTO) {
        AuthTokenDTO tokenDTO = authService.login(loginUserDTO);
        return ResponseEntity.ok(tokenDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenDTO> refresh(@RequestBody RefreshRequest body) {
        AuthTokenDTO tokenDTO = authService.refresh(body.refreshToken());
        return ResponseEntity.ok(tokenDTO);
    }
}
