package chat.auth_service.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chat.auth_service.dto.request.ChangePasswordRequest;
import chat.auth_service.dto.request.UpdateProfileRequest;
import chat.auth_service.dto.response.UserResponseDTO;
import chat.auth_service.entity.User;
import chat.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public User findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public User findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(UserResponseDTO::from);
    }

    @Transactional
    public User updateProfile(String email, UpdateProfileRequest req) {
        var user = findByEmail(email);
        user.setUsername(req.username());
        return repository.save(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        var user = findByEmail(email);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Senha atual incorreta");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        repository.save(user);
    }

    public void disable(UUID id) {
        var user = findById(id);
        user.setEnabled(false);
        repository.save(user);
    }
}
