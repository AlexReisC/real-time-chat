package chat.chat_service.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {
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

    private Date getExpirationFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigninKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Erro ao analisar a data de expiração. JWT inválido.", e);
        }
    }

    public Boolean isTokenValid(String token) {
        final Date expiration = getExpirationFromToken(token);
        return !expiration.before(new Date());
    }
}