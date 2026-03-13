package chat.auth_service.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chat.auth_service.dto.request.CreateUserDTO;
import chat.auth_service.dto.request.LoginUserDTO;
import chat.auth_service.dto.response.AuthTokenDTO;
import chat.auth_service.entity.Role;
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

    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    public AuthTokenDTO login(LoginUserDTO loginUserDTO) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginUserDTO.email(),
                loginUserDTO.password()
        );

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        UserDetails user = (UserDetails) authenticate.getPrincipal();

        return issueTokens(user);
    }

    public AuthTokenDTO createUser(CreateUserDTO createUserDTO) {
        User user;
        try {
            user = User.builder()
                    .email(createUserDTO.email())
                    .password(passwordEncoder.encode(createUserDTO.password()))
                    .username(createUserDTO.username())
                    .roles(Set.of(Role.ROLE_USER))
                    .build();

            var saved = userRepository.save(user);
            return issueTokens(saved);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException("O email já está em uso");
        }
    }

    public AuthTokenDTO refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido");
        }

        String email = jwtService.getSubjectFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expirado");
        }

        return issueTokens(user);
    }

    private AuthTokenDTO issueTokens(UserDetails user) {
        return AuthTokenDTO.of(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtExpiration);
    }
}
