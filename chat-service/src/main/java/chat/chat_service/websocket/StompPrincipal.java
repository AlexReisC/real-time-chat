package chat.chat_service.websocket;

import java.security.Principal;

public record StompPrincipal(String userId) implements Principal {
    @Override
    public String getName() {
        return userId;
    }
}
