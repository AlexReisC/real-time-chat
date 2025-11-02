package chat.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRoomDTO(@NotBlank(message = "O título da sala é obrigatório") String title) {
}
