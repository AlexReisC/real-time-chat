package chat.chat_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chat.chat_service.dto.request.ConversationSummaryDTO;
import chat.chat_service.dto.response.PageResponseDTO;
import chat.chat_service.dto.response.ResponseMessageDTO;
import chat.chat_service.service.MessageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/messages")
@Validated
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/room/{roomId}/history")
    public ResponseEntity<PageResponseDTO<ResponseMessageDTO>> listAllMessagesByRoom(
            @PathVariable @NotBlank(message = "O ID da sala é obrigatório") String roomId,
            @Min(value = 0) @Max(value = 100) @RequestParam(defaultValue = "1") int page,
            @Min(value = 1) @Max(value = 100) @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy
    ) {
        PageResponseDTO<ResponseMessageDTO> allMessages = messageService.listAllMessages(roomId, PageRequest.of(page, size, Sort.by(sortBy)));
        return ResponseEntity.ok(allMessages);
    }

    @GetMapping("/private/{targetUserId}/history")
    public ResponseEntity<PageResponseDTO<ResponseMessageDTO>> listAllPrivateMessages (
            @PathVariable String targetUserId,
            @AuthenticationPrincipal Jwt jwt,
            @Min(value = 0) @Max(value = 100) @RequestParam(defaultValue = "1") int page,
            @Min(value = 1) @Max(value = 100) @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy
    ) {
        String currentUserId = jwt.getClaimAsString("userId");
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortBy));
        PageResponseDTO<ResponseMessageDTO> allPrivateMessages = messageService.listAllPrivateMessages(currentUserId, targetUserId, pageRequest);
        return ResponseEntity.ok(allPrivateMessages);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ResponseMessageDTO>> getRoomHistory(@NotBlank(message = "O ID da sala é obrigatório") @PathVariable String roomId) {
        List<ResponseMessageDTO> history = messageService.getRecentRoomMessages(roomId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/private/{targetUserId}")
    public ResponseEntity<List<ResponseMessageDTO>> getPrivateMessages(@PathVariable String targetUserId, @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = jwt.getClaimAsString("userId");
        List<ResponseMessageDTO> history = messageService.getRecentPrivateMessages(targetUserId, currentUserId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/private/conversations")
    public ResponseEntity<List<ConversationSummaryDTO>> listActiveConversations(
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Extrai o ID do utilizador autenticado a partir do token
        String currentUserId = jwt.getClaimAsString("userId");
        
        // Chama o serviço para obter a lista de resumos
        List<ConversationSummaryDTO> conversations = messageService.getUserPrivateConversations(currentUserId);
        
        return ResponseEntity.ok(conversations);
    }
}
