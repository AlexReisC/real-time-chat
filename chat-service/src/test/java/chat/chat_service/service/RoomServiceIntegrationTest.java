package chat.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RoomServiceIntegrationTest {
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @BeforeEach
    void cleanUp() {
        roomRepository.deleteAll();
    }

    @Test
    void shouldCreateNewRoomSuccessfullyThenReturnRoom() {
        Room room = roomService.createNewRoom("Sala de Teste");

        Optional<Room> encontrada = roomRepository.findById(room.getId());

        assertTrue(encontrada.isPresent());
        assertEquals("Sala de Teste", encontrada.get().getTitle());
        assertTrue(encontrada.get().getMembersIds().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenCreatingRoomWithSameTitle() {
        roomService.createNewRoom("Duplicada");

        EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.createNewRoom("Duplicada");
        });

        assertEquals("Já existe uma sala com o mesmo título", exception.getMessage());
    }

    @Test
    void shouldAddUserInExistingRoom() {
        Room room = roomService.createNewRoom("sala de usuario");

        roomService.addNewUser(room.getId(), "user-123");

        Room updated = roomRepository.findById(room.getId()).orElseThrow();

        assertTrue(updated.getMembersIds().contains(("user-123")));
    }

    @Test
    void shouldThrowExceptionWhenAddExistingUser() {
        Room room = roomService.createNewRoom("sala duplicada");

        roomService.addNewUser(room.getId(), "user-123");

        EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.addNewUser(room.getId(), "user-123");
        });

        assertEquals("Usuário já é membro da sala", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAddUserInNonexistentRoom() {
        RoomNotFoundException exception = assertThrows(RoomNotFoundException.class, () -> {
            roomService.addNewUser("111", "user-456");
        });

        assertEquals("Sala não encontrada", exception.getMessage());
    }

    @Test
    void shouldRemoveUserFromExistingRoom() {
        Room room = roomService.createNewRoom("sala para remover usuario");
        
        roomService.addNewUser(room.getId(), "user-789");
        roomService.removeUser(room.getId(), "user-789");
        
        Room updated = roomRepository.findById(room.getId()).orElseThrow();

        assertTrue(updated.getMembersIds().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenRemoveUserFromNonexistentRoom() {
        RoomNotFoundException exception = assertThrows(RoomNotFoundException.class, () -> {
            roomService.removeUser("17700", null);
        });

        assertEquals("Sala não encontrada", exception.getMessage());
    }

    @Test
    void shouldListAllRoomsWithPagination() {
        roomService.createNewRoom("Sala 1");
        roomService.createNewRoom("Sala 2");

        PageResponseDTO<Room> page = roomService.listAllRooms(Pageable.ofSize(1).withPage(0));

        assertEquals(1, page.content().size());
        assertEquals(0, page.pageNumber());
        assertEquals(2, page.totalPages());
        assertEquals(2, page.totalElements());
    }
}