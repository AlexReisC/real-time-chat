package chat.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.result.UpdateResult;

import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private RoomService roomService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .id("1")
                .title("Geral")
                .membersIds(new HashSet<>())
                .build();
    }

    @Test
    void shouldCreateRoomSuccessfully() {
        String title = "Sala de Teste";

        when(mongoTemplate.findOne(any(), eq(Room.class))).thenReturn(null);
        when(mongoTemplate.insert(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = roomService.createNewRoom(title);

        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertTrue(result.getMembersIds().isEmpty());
        verify(mongoTemplate).insert(any(Room.class));
    }

    @Test
    void shouldThrowsExceptionWhenRoomWithSameTitleAlreadyExists() {
        String title = "Geral";
        when(mongoTemplate.findOne(any(), eq(Room.class))).thenReturn(room);

        assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.createNewRoom(title);
        });

        verify(mongoTemplate, never()).insert(any(Room.class));
    }

    @Test
    void shouldAddNewUserInTheRoomSuccessfully() {
        String roomId = "1";
        String userId = "user-456";

        UpdateResult successResult = mock(UpdateResult.class);
        when(successResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Room.class)))
            .thenReturn(successResult);

        assertDoesNotThrow(() -> roomService.addNewUser(roomId, userId));
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Room.class));
    }

    @Test
    void shouldThrowRoomNotFoundExceptionWhenRoomDoesntExists() {
        String roomId = "999";
        String userId = "user-123";

        UpdateResult failResult = mock(UpdateResult.class);
        when(failResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Room.class)))
            .thenReturn(failResult);
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        RoomNotFoundException ex = assertThrows(RoomNotFoundException.class, () -> {
            roomService.addNewUser(roomId, userId);
        });

        assertEquals("Sala não encontrada!", ex.getMessage());
    }

    @Test
    void shouldThrowEntityAlreadyExistsExceptionWhenUserIsMenberOfRoom() {
        String roomId = "1";
        String userId = "user-123";
        room.setMembersIds(Set.of(userId));

        UpdateResult failResult = mock(UpdateResult.class);
        when(failResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Room.class)))
            .thenReturn(failResult);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        EntityAlreadyExistsException ex = assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.addNewUser(roomId, userId);
        });

        assertEquals("Usuário já é membro da sala", ex.getMessage());    
    }

    @Test
    void shouldRemoveUserFromRoomSuccessfully() {
        String roomId = "1";
        String userId = "user-123";
        
        UpdateResult successResult = mock(UpdateResult.class);
        when(successResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Room.class)))
            .thenReturn(successResult);
        
        assertDoesNotThrow(() -> roomService.removeUser(roomId, userId));
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Room.class));
    }

    @Test
    void shouldThrowRoomNotFoundExceptionWhenRemovingUserFromNonexistentRoom() {
        String roomId = "999";
        String userId = "user-123";
        
        UpdateResult failResult = mock(UpdateResult.class);
        when(failResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Room.class)))
            .thenReturn(failResult);
        
        RoomNotFoundException ex = assertThrows(RoomNotFoundException.class, () -> {
            roomService.removeUser(roomId, userId);
        });
        assertEquals("Sala não encontrada", ex.getMessage());
    }

    @Test
    void shouldReturnPageOfRooms() {
        Room room1 = Room.builder().id("1").title("Sala 1").membersIds(new HashSet<>()).build();
        Room room2 = Room.builder().id("2").title("Sala 2").membersIds(new HashSet<>()).build();
        PageImpl<Room> rooms = new PageImpl<>(List.of(room1, room2));

        when(roomRepository.findAll(PageRequest.of(0, 10))).thenReturn(rooms);
        var result = roomService.listAllRooms(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Sala 1", result.content().get(0).getTitle());
        assertEquals("Sala 2", result.content().get(1).getTitle());
    }
}