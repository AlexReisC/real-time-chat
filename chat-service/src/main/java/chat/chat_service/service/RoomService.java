package chat.chat_service.service;

import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;
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

    public List<Room> listAllRooms() {
        return roomRepository.findAll();
    }

    public List<String> listAllMembersByRoom(String roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        return new ArrayList<>(room.getMembersIds());
    }

}
