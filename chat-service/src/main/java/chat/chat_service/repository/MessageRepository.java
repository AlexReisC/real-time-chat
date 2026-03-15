package chat.chat_service.repository;

import chat.chat_service.model.Message;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByRoomId(String roomId, Pageable pageable);
    
    @Query("{ $and: [ { 'roomId': null }, { $or: [ { 'senderId': ?0, 'recipientId': ?1 }, { 'senderId': ?1, 'recipientId': ?0 } ] } ] }")
    Page<Message> findPrivateConversation(String userId1, String userId2, Pageable pageable);

    List<Message> findTop50ByRoomIdOrderByTimestampDesc(String roomId);
    
    @Query(value = "{ $or: [ { 'senderId': ?0, 'recipientId': ?1 }, { 'senderId': ?1, 'recipientId': ?0 } ] }", sort = "{ 'timestamp': -1 }")
    List<Message> findTop50PrivateConversation(String userId1, String userId2);
}
