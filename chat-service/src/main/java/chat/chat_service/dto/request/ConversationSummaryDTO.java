package chat.chat_service.dto.request;

import java.time.Instant;

public record ConversationSummaryDTO(
        String contactId,
        String contactUsername,
        String lastMessage,
        Instant timestamp
) { }
