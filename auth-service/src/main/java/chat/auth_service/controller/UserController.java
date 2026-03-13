package chat.auth_service.controller;


import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chat.auth_service.dto.request.ChangePasswordRequest;
import chat.auth_service.dto.request.UpdateProfileRequest;
import chat.auth_service.dto.response.UserResponseDTO;
import chat.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe( @Valid @RequestBody UpdateProfileRequest request, Authentication auth) {
        var updated = userService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(UserResponseDTO.from(updated));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication auth) {
        userService.changePassword(auth.getName(), req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable UUID id) {
        var user = userService.findById(id);
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        userService.disable(id);
        return ResponseEntity.noContent().build();
    }
}
