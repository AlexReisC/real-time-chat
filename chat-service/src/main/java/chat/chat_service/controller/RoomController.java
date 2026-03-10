package chat.chat_service.controller;

import chat.chat_service.dto.request.CreateRoomDTO;
import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.model.Room;
import chat.chat_service.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
    public ResponseEntity<PageResponseDTO<Room>> listAllRooms(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "title") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Direction direction = "desc".equalsIgnoreCase(sortDir) ? Direction.DESC : Direction.ASC;
        PageResponseDTO<Room> roomPage = roomService.listAllRooms(PageRequest.of(page, size, direction, sortBy));
        return ResponseEntity.ok(roomPage);
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<PageResponseDTO<String>> listAllMembers(
        @PathVariable @NotBlank(message = "O ID da sala é obrigatório") String roomId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Direction direction = "desc".equalsIgnoreCase(sortDir) ? Direction.DESC : Direction.ASC;
        PageResponseDTO<String> membersPage = roomService.listAllMembersByRoom(roomId, PageRequest.of(page, size, direction));
        return ResponseEntity.ok(membersPage);
    }
}
