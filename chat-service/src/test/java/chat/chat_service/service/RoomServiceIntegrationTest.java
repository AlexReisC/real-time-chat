package chat.chat_service.service;

import chat.chat_service.exception.EntityAlreadyExistsException;
import chat.chat_service.exception.RoomNotFoundException;
import chat.chat_service.model.Room;
import chat.chat_service.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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

    @Test
    void shouldSaveNewRoom_ThenReturnRoomSaved() {
        Room room = roomService.createNewRoom("Sala de Teste");

        Optional<Room> encontrada = roomRepository.findById(room.getId());

        assertTrue(encontrada.isPresent());
        assertEquals("Sala de Teste", encontrada.get().getTitle());
        assertTrue(encontrada.get().getMembersUsernames().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenCreatingRoomWithSameTitle() {
        roomService.createNewRoom("Duplicada");

        EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.createNewRoom("Duplicada");
        });
    }

    @Test
    void shouldAddUserInExistingRoom() {
        Room room = roomService.createNewRoom("sala de usuario");

        roomService.addNewUser(room.getId(), "alex");

        Room updated = roomRepository.findById(room.getId()).orElseThrow();

        assertTrue(updated.getMembersUsernames().contains(("alex")));
    }

    @Test
    void shouldThrowExceptionWhenAddExistingUser() {
        Room room = roomService.createNewRoom("sala duplicada");

        roomService.addNewUser(room.getId(), "alex");

        assertThrows(EntityAlreadyExistsException.class, () -> {
            roomService.addNewUser(room.getId(), "alex");
        });
    }

    @Test
    void shouldThrowExceptionWhenAddUserInNonexistentRoom() {
        assertThrows(RoomNotFoundException.class, () -> {
            roomService.addNewUser("111", "alex");
        });
    }
}