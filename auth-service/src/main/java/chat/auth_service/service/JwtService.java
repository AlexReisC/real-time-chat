package chat.auth_service.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
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

    public String generateToken(String username) {
        try {
            return Jwts.builder()
                    .issuer(ISSUER)
                    .subject(username)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(getSigninKey())
                    .compact();
        } catch (InvalidKeyException e) {
            throw new InvalidKeyException("Erro ao gerar token, erro na chave de assinatura.");
        }
    }


}
