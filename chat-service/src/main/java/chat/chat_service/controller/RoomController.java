package chat.chat_service.controller;

import chat.chat_service.dto.request.CreateRoomDTO;
import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.dto.response.UserNotificationResponseDTO;
import chat.chat_service.model.NotificationType;
import chat.chat_service.model.Room;
import chat.chat_service.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/rooms")
@Validated
public class RoomController {
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomController(RoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<Room> createNewRoom(@Valid  @RequestBody CreateRoomDTO createRoomDTO) {
        Room room = roomService.createNewRoom(createRoomDTO.title());
        return ResponseEntity.created(URI.create("/chat/rooms/" + room.getId())).body(room);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<Room>> listAllRooms(
        @Min(value = 0) @Max(value = 100) @RequestParam(defaultValue = "0") int page,
        @Min(value = 1) @Max(value = 100) @RequestParam(defaultValue = "20") int size,
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
        @Min(value = 0) @Max(value = 100) @RequestParam(defaultValue = "0") int page,
        @Min(value = 1) @Max(value = 100) @RequestParam(defaultValue = "20") int size
    ) {
        PageResponseDTO<String> membersPage = roomService.listAllMembersByRoom(roomId, PageRequest.of(page, size));
        return ResponseEntity.ok(membersPage);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable @NotBlank(message = "O ID da sala é obrigatório") String roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}/members")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable @NotBlank(message = "O ID da sala é obrigatório") String roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String currentUserId = jwt.getClaimAsString("userId");
        String currentUsername = jwt.getClaimAsString("userDisplayName");

        roomService.removeUser(roomId, currentUserId);

        UserNotificationResponseDTO leaveNotification = new UserNotificationResponseDTO(
                NotificationType.LEAVE,
                currentUserId,
                currentUsername,
                roomId,
                currentUsername + " saiu da sala.",
                Instant.now()
        );
        
        messagingTemplate.convertAndSend("/topic/rooms." + roomId, leaveNotification);

        return ResponseEntity.noContent().build();
    }
}
