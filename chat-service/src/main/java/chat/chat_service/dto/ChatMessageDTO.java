package chat.chat_service.dto;

import chat.chat_service.model.Message;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ChatMessageDTO(
        @JsonProperty("roomId") String roomId,
        @JsonProperty("content") String content,
        @JsonProperty("recipientId") String recipientId,
        @JsonProperty("timestamp") Instant timestamp
) {
    public Message toEntity(String senderId, String senderUsername) {
        Message message = new Message();
        message.setContent(this.content);
        message.setSenderId(senderId);
        message.setSenderUsername(senderUsername);
        message.setRecipientId(this.recipientId);
        message.setTimestamp(this.timestamp);
        return message;
    }
}
