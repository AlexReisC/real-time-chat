package chat.chat_service.dto.request;

import java.time.Instant;

public record ConversationSummaryDTO(
        String userId,
        String username,
        String lastMessage,
        Instant timestamp
) { }
