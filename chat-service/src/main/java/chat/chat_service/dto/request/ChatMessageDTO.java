package chat.chat_service.dto.request;

import chat.chat_service.model.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ChatMessageDTO(
        @NotBlank(message = "O ID da sala é obrigatório")
        @JsonProperty("roomId") String roomId,

        @NotBlank(message = "O conteúdo não pode estar em branco")
        @Size(max = 1024, message = "A mensagem não pode exceder 1024 caracteres")
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
