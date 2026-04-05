package chat.chat_service.service;

import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Message;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public RoomService(RoomRepository roomRepository, MongoTemplate mongoTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.roomRepository = roomRepository;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
    }

    public void existById(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException("Sala não encontrada");
        }
    }

    public Room createNewRoom(String title){
        Room existingRoom = mongoTemplate.findOne(Query.query(Criteria.where("title").is(title)), Room.class);
        
        if (existingRoom != null) {
            throw new EntityAlreadyExistsException("Já existe uma sala com o mesmo título");
        }
        
        Set<String> membersIds = new HashSet<>();
        Room room = Room.builder().title(title).membersIds(membersIds).build();
        return mongoTemplate.insert(room);
    }

    public Room addNewUser(String roomId, String userId){
        UpdateResult result = mongoTemplate.updateFirst(
            Query.query(Criteria.where("id").is(roomId).and("membersIds").ne(userId)),
            new Update().addToSet("membersIds", userId),
            Room.class
        );

        if (result.getMatchedCount() == 0) {
            Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));
            if (room.getMembersIds().contains(userId)) {
                throw new EntityAlreadyExistsException("Usuário já é membro da sala");
            }
            return room;
        }

        return roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));
    }

    public void removeUser(String roomId, String userId) {
        UpdateResult updateFirst = mongoTemplate.updateFirst(
            Query.query(Criteria.where("id").is(roomId).and("membersIds").is(userId)),
            new Update().pull("membersIds", userId),
            Room.class
        );

        if (updateFirst.getMatchedCount() == 0) {
            throw new RoomNotFoundException("Sala não encontrada");
        }
    }

    public PageResponseDTO<Room> listAllRooms(Pageable pageable) {
        Page<Room> roomPage = roomRepository.findAll(pageable);
        return new PageResponseDTO<>(
                roomPage.getContent(),
                roomPage.getNumber(),
                roomPage.getTotalPages(),
                roomPage.getTotalElements(),
                roomPage.getSize()
        );
    }

    public PageResponseDTO<String> listAllMembersByRoom(String roomId, Pageable pageable) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        List<String> membersList = new ArrayList<>(room.getMembersIds());
        int total = membersList.size();
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        
        List<String> pageContent = membersList.subList(start, end);
        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());
        
        return new PageResponseDTO<>(
            pageContent,
            pageable.getPageNumber(),
            totalPages,
            total,
            pageable.getPageSize()
        );
    }

    public void deleteRoom(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException("Sala não encontrada");
        }
        
        roomRepository.deleteById(roomId);

        Query deleteMessagesQuery = new Query(Criteria.where("roomId").is(roomId));
        mongoTemplate.remove(deleteMessagesQuery, Message.class);
        
        String cacheKey = "room:" + roomId + ":messages";
        redisTemplate.delete(cacheKey);
    }
}
