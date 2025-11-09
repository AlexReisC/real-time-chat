package chat.auth_service.service;

import chat.auth_service.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private static final String ISSUER = "auth-service";

    private SecretKey getSigninKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(User user) {
        try {
            return Jwts.builder()
                    .issuer(ISSUER)
                    .subject(user.getUsername())
                    .claim("userId", user.getId())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(getSigninKey())
                    .compact();
        } catch (InvalidKeyException e) {
            throw new InvalidKeyException("Erro ao gerar token, erro na chave de assinatura.");
        }
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
}
