package chat.auth_service.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import chat.auth_service.dto.request.ChangePasswordRequest;
import chat.auth_service.dto.request.UpdateProfileRequest;
import chat.auth_service.dto.response.UserResponseDTO;
import chat.auth_service.entity.User;
import chat.auth_service.exception.InvalidPasswordException;
import chat.auth_service.exception.UserNotFoundException;
import chat.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public User findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    public User findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(UserResponseDTO::from);
    }

    @Transactional
    public User updateProfile(String email, UpdateProfileRequest req) {
        var user = findByEmail(email);
        user.setDisplayName(req.username());
        User updatedUser = repository.save(user);

        String redisKey = "user:" + updatedUser.getId() + ":username";
        redisTemplate.opsForValue().set(redisKey, req.username());

        return updatedUser;
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        var user = findByEmail(email);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Senha atual incorreta");
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
