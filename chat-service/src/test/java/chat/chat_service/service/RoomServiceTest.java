package chat.chat_service.service;

import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .id("1")
                .title("General")
                .membersUsernames(new HashSet<>())
                .build();
    }

    @Test
    void shouldCreateRoomWhenTitleNotExists() {
        // given
        String title = "Nova Sala";
        when(roomRepository.existsByTitle(title)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Room result = roomService.createNewRoom(title);

        // then
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertTrue(result.getMembersUsernames().isEmpty());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void shouldThrowsExceptionWhenRoomWithSameTitleAlreadyExists() {
        // given
        String title = "Sala Existente";
        when(roomRepository.existsByTitle(title)).thenReturn(true);

        // then
        EntityAlreadyExistsException ex = assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.createNewRoom(title);
        });

        assertEquals("Já existe uma sala com o mesmo título", ex.getMessage());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void shouldAddNewUserInTheRoom() {
        // given
        String roomId = "1";
        String username = "alex";
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        roomService.addNewUser(roomId, username);

        // then
        assertTrue(room.getMembersUsernames().contains(username));
        verify(roomRepository).save(room);
    }

    @Test
    void shouldThrowsExceptionWhenRoomDoesntExists() {
        // given
        when(roomRepository.findById("999")).thenReturn(Optional.empty());

        // then
        RoomNotFoundException ex = assertThrows(RoomNotFoundException.class, () -> {
            roomService.addNewUser("999", "alex");
        });

        assertEquals("Sala não encontrada!", ex.getMessage());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void shouldThrowsExceptionWhenUserAlreadyExistsInRoom() {
        // given
        String roomId = "1";
        String username = "alex";
        Set<String> membros = new HashSet<>();
        membros.add(username);
        room.setMembersUsernames(membros);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // then
        EntityAlreadyExistsException ex = assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.addNewUser(roomId, username);
        });

        assertEquals("Já existe um usuário com este username na sala.", ex.getMessage());
        verify(roomRepository, never()).save(any());
    }
}