package chat.chat_service.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    public SecretKey getSigninKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String getSubjectFromToken(String token){
        try {
            return Jwts.parser()
                    .verifyWith(getSigninKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Erro ao analisar as claims. JWT inválido.");
        }
    }

    public String getUserIdFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigninKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("userId", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Erro ao analisar as claims. JWT inválido.");
        }
    }

    public boolean isTokenValid(String token) {
    try {
        Jwts.parser()
            .verifyWith(getSigninKey())
            .build()
            .parseSignedClaims(token); // valida assinatura + expiração juntos
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        logger.warn("Token inválido: {}", e.getMessage());
        return false;
    }
}
}