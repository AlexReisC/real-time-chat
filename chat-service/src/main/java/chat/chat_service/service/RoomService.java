package chat.chat_service.service;

import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.dto.response.RoomMemberDTO;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;

    public RoomService(RoomRepository roomRepository, MongoTemplate mongoTemplate, StringRedisTemplate redisTemplate) {
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
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("id").is(roomId).and("membersIds").ne(userId)),
            new Update().addToSet("membersIds", userId),
            Room.class
        );

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

    public PageResponseDTO<RoomMemberDTO> listAllUsersByRoom(String roomId, Pageable pageable) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        Set<String> memberIds = room.getMembersIds();

        if (memberIds == null || memberIds.isEmpty()) {
            return new PageResponseDTO<>(Collections.emptyList(), pageable.getPageNumber(), 0, 0, pageable.getPageSize());
        }

        List<String> redisKeys = memberIds.stream()
            .map(id -> "user:" + id + ":username")
            .collect(Collectors.toList());

        List<String> usernamesFromRedis = redisTemplate.opsForValue().multiGet(redisKeys);
        
        List<RoomMemberDTO> membersList = new ArrayList<>();
        int index = 0;
        for (String memberId : memberIds) {
            String username = "Usuário Desconecido";
            if (usernamesFromRedis != null && usernamesFromRedis.get(index) != null) {
                username = usernamesFromRedis.get(index);
            }
            membersList.add(new RoomMemberDTO(memberId, username));
            index++;
        }
        
        int size = membersList.size();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), size);
        List<RoomMemberDTO> pagedMembers = membersList.subList(start, end);
        return new PageResponseDTO<>(
            pagedMembers,
            pageable.getPageNumber(),
            (int) Math.ceil((double) size / pageable.getPageSize()),
            size,
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
