package chat.chat_service.controller;

import chat.chat_service.dto.request.CreateRoomDTO;
import chat.chat_service.model.Room;
import chat.chat_service.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<Room> createNewRoom(@Valid  @RequestBody CreateRoomDTO createRoomDTO) {
        Room room = roomService.createNewRoom(createRoomDTO.title());
        return ResponseEntity.created(URI.create("/chat/rooms/" + room.getId())).body(room);
    }

    @GetMapping
    public ResponseEntity<List<Room>> listAllRooms() {
        List<Room> roomList = roomService.listAllRooms();
        return ResponseEntity.ok(roomList);
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<String>> listAllMembers(@PathVariable @NotBlank(message = "O ID da sala é obrigatório") String roomId) {
        List<String> members = roomService.listAllMembersByRoom(roomId);
        return ResponseEntity.ok(members);
    }
}
