package chat.chat_service.controller;

import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.model.Message;
import chat.chat_service.service.MessageService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<PageResponseDTO<Message>> listAllMessagesByRoom(
            @PathVariable @NotBlank(message = "O ID da sala é obrigatório") String roomId,
            @DecimalMin(value = "0", message = "O número da página deve ser zero ou maior") @RequestParam(defaultValue = "0") int page,
            @DecimalMin(value = "1", message = "O tamanho da página deve ser maior ou igual a 1") @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy
            ) {
        PageResponseDTO<Message> allMessages = messageService.listAllMessages(roomId, PageRequest.of(page, size, Sort.by(sortBy)));
        return ResponseEntity.ok(allMessages);
    }

    @GetMapping("/private")
    public ResponseEntity<PageResponseDTO<Message>> listAllPrivateMessages(
            @RequestParam @NotBlank(message = "O ID do usuário 1 é obrigatório") String user1,
            @RequestParam @NotBlank(message = "O ID do usuário 2 é obrigatório") String user2,
            @DecimalMin(value = "0", message = "O número da página deve ser zero ou maior") @RequestParam(defaultValue = "0") int page,
            @DecimalMin(value = "1", message = "O tamanho da página deve ser maior ou igual a 1") @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy
            ) {
        PageResponseDTO<Message> allPrivateMessages = messageService.listAllPrivateMessages(user1, user2, PageRequest.of(page, size, Sort.by(sortBy)));
        return ResponseEntity.ok(allPrivateMessages);
    }
}
