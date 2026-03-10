package chat.chat_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
@CompoundIndex(name = "room_timestamp_idx", def = "{'room_id': 1, 'timestamp': -1}")
public class Message {
    @Id
    private String id;
    
    @Field("room_id")
    private String roomId;

    @Field("sender_id")
    @Indexed(name = "sender_id")
    private String senderId;
    
    @Field("sender_username")
    private String senderUsername;
    
    @Field("recipient_id")
    @Indexed(name = "recipient_id")
    private String recipientId;
    
    private String content;
    
    private Instant timestamp;
}
