package chat.auth_service.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import chat.auth_service.dto.request.CreateUserDTO;
import chat.auth_service.dto.request.LoginUserDTO;
import chat.auth_service.dto.response.RecoveryTokenDTO;
import chat.auth_service.dto.response.UserResponseDTO;
import chat.auth_service.entity.User;
import chat.auth_service.exception.EmailAlreadyExistsException;
import chat.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RecoveryTokenDTO authenticateUser(LoginUserDTO loginUserDTO) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginUserDTO.email(),
                loginUserDTO.password()
        );

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        User user = (User) authenticate.getPrincipal();

        return new RecoveryTokenDTO(jwtService.generateToken(user));
    }

    public UserResponseDTO createUser(CreateUserDTO createUserDTO) {
        boolean emailExists = userRepository.findByEmail(createUserDTO.email()).isPresent();
        if (emailExists) {
            throw new EmailAlreadyExistsException("O email já está em uso");
        }

        User user = User.builder()
                .email(createUserDTO.email())
                .password(passwordEncoder.encode(createUserDTO.password()))
                .username(createUserDTO.username())
                .build();

        User saved = userRepository.save(user);
        return new UserResponseDTO(
                saved.getId(),
                saved.getEmail(),
                saved.getUsername()
        );
    }
}
