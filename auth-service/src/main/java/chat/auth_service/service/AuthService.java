package chat.auth_service.service;

import chat.auth_service.config.SecurityConfig;
import chat.auth_service.dto.request.CreateUserDTO;
import chat.auth_service.dto.request.LoginUserDTO;
import chat.auth_service.dto.response.RecoveryTokenDTO;
import chat.auth_service.dto.response.UserResponseDTO;
import chat.auth_service.entity.User;
import chat.auth_service.exception.EmailAlreadyExistsException;
import chat.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SecurityConfig securityConfig;

    public RecoveryTokenDTO authenticateUser(LoginUserDTO loginUserDTO) {
        userRepository.findByEmail(loginUserDTO.email()).orElseThrow(() ->
                new UsernameNotFoundException("Email não encontrado"));

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
                .password(securityConfig.passwordEncoder().encode(createUserDTO.password()))
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
