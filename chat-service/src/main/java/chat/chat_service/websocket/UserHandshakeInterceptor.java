package chat.chat_service.websocket;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import chat.chat_service.security.JwtService;

import java.util.Map;

@Component
public class UserHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtService jwtService;
    
    public UserHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    private static final Logger logger = LoggerFactory.getLogger(UserHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        logger.info("Iniciando Handshake Websocket");

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String token = httpRequest.getParameter("token");

            if (token != null && jwtService.isTokenValid(token)) {
                String userId = jwtService.getUserIdFromToken(token);
                String username = jwtService.getSubjectFromToken(token);

                if (userId == null || username == null) {
                     logger.warn("Handshake rejeitado - Token válido, mas claims ausentes.");
                     return false;
                }

                attributes.put("userId", userId);
                attributes.put("username", username);
                
                logger.info("Handshake aceito - UserId: {}, Username: {}", userId, username);
                return true;
            } else {
                logger.warn("Handshake rejeitado - Token ausente ou inválido.");
                return false;
            }
        }

        logger.warn("Handshake rejeitado - Request não é HTTP");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("Erro durante o handshake: {}", exception.getMessage(), exception);
        } else {
            logger.info("Handshake completado com sucesso para a sessão.");
        }
    }
}
