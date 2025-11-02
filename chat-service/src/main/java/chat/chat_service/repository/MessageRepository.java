package chat.chat_service.repository;

import chat.chat_service.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByRoomId(String roomId, Pageable pageable);
    @Query("{$and: [ {roomId: null}, {$or: [ {senderId: ?0, recipientId: ?1}, {senderId: ?1, recipientId: ?0} ]} ]}")
    Page<Message> findPrivateConversation(String userId1, String userId2, Pageable pageable);
}
