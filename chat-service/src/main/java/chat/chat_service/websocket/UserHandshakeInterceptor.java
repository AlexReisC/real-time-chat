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

import java.util.Map;

@Component
public class UserHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(UserHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        logger.info("Iniciando Handshake Websocket");

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            String userId = httpRequest.getHeader("X-User-Id");
            String username = httpRequest.getHeader("X-Username");

            if (userId != null && username != null) {
                attributes.put("userId", userId);
                attributes.put("username", username);
                logger.info("Handshake aceito - UserId: {}, Username: {}", userId, username);
                return true;
            }
        } else {
            logger.warn("Handshake rejeitado - Informações de usuário ausentes");
            return false;
        }

        logger.warn("Handshake rejeitado - Request inválido");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("Erro durante o handshake: {}", exception.getMessage(), exception);
        } else {
            logger.info("Handshake completado com sucesso");

            // Aqui você pode fazer logs adicionais ou outras operações pós-handshake
            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getServletRequest();

                String userId = httpRequest.getHeader("X-User-Id");
                String username = httpRequest.getHeader("X-Username");

                logger.info("Conexão WebSocket estabelecida - UserId: {}, Username: {}, IP: {}",
                        userId, username, httpRequest.getRemoteAddr());
            }
        }
    }
}
