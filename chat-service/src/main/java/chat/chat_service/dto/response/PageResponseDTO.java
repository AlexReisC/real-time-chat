package chat.chat_service.dto.response;

import java.util.List;

public record PageResponseDTO<T> (
        List<T> messages,
        int pageNumber,
        int totalPages,
        long totalElements,
        int size
) {
}
