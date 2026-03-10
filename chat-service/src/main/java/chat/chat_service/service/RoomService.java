package chat.chat_service.service;

import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void existById(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException("Sala não encontrada!");
        }
    }

    public Room createNewRoom(String title){
        if (roomRepository.existsByTitle(title)) {
            throw new EntityAlreadyExistsException("Já existe uma sala com o mesmo título");
        }
        Set<String> membersIds = new HashSet<>();
        Room room = Room.builder().title(title).membersIds(membersIds).build();
        return roomRepository.save(room);
    }

    public void addNewUser(String roomId, String userId){
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException("Sala não encontrada!"));
        if (room.getMembersIds().contains(userId)){
            throw new EntityAlreadyExistsException("Já existe um usuário com este ID na sala.");
        }
        room.getMembersIds().add(userId);
        roomRepository.save(room);
    }

    public void removeUser(String roomId, String userId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));
        boolean removed = room.getMembersIds().removeIf(u -> u.equals(userId));
        if (removed){
            roomRepository.save(room);
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

}
