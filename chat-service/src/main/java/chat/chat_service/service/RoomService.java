package chat.chat_service.service;

import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void addNewUser(String roomId, String username){
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException("Sala não encontrada!"));
        if (room.getMembersUsernames().contains(username)){
            throw new EntityAlreadyExistsException("Já existe um usuário com este username na sala.");
        }
        room.getMembersUsernames().add(username);
        roomRepository.save(room);
    }
}
