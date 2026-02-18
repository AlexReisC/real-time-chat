package chat.chat_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    
    @Indexed(name = "room_id")
    private String roomId;

    @Indexed
    private String senderId;
    
    private String senderUsername;
    
    @Indexed
    private String recipientId;
    
    private String content;
    
    private Instant timestamp;
}
