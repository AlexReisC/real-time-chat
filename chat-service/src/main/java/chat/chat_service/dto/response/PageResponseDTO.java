package chat.chat_service.dto.response;

import java.util.List;

public record PageResponseDTO<T> (
        List<T> content,
        int pageNumber,
        int totalPages,
        long totalElements,
        int size
) {
}
