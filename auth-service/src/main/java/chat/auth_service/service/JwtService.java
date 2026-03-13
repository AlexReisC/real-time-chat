package chat.auth_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpirationMs;

    private static final String ISSUER = "auth-service";

    @PostConstruct
    public void validateKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "jwt.secret deve ter no mínimo 32 caracteres (256 bits). Atual: " + keyBytes.length
            );
        }
    }

    private SecretKey getSigninKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserDetails user) {
        return buildToken(user, jwtExpirationMs, Map.of("type", "access"));
    }

    public String generateRefreshToken(UserDetails user) {
        return buildToken(user, jwtRefreshExpirationMs, Map.of("type", "refresh"));
    }

    private String buildToken(UserDetails user, long jwtExpirationMs, Map<String, Object> additionalClaims) {
        try {
            var now = Instant.now();
            return Jwts.builder()
                    .issuer(ISSUER)
                    .subject(user.getUsername())
                    .claims(additionalClaims)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusMillis(jwtExpirationMs)))
                    .signWith(getSigninKey())
                    .compact();
        } catch (InvalidKeyException e) {
            throw new InvalidKeyException("Erro ao gerar token. Chave de assinatura inválida.");
        }
    }

    public String getSubjectFromToken(String token){
        return parseClaims(token).getSubject();
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", String.class);
    }

    private Boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    public Boolean isTokenValid(String token, UserDetails userDetails) {
        final String usernameFromToken = getSubjectFromToken(token);
        return (usernameFromToken.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("type", String.class));
    }

    public long getExpiration(){
        return this.jwtExpirationMs;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigninKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Erro ao analisar as claims. JWT inválido.", e);
        }
    }
}
