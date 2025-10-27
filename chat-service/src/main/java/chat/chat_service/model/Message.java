package chat.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collation = "messages")
public class Message {
    @Id
    private String id;
    @DBRef
    private String roomId;
    private String sender;
    private String recipient;
    private String content;
    private String timestamp;
}
