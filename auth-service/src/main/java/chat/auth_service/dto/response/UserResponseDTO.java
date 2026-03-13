package chat.auth_service.dto.response;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import chat.auth_service.entity.Role;
import chat.auth_service.entity.User;

public record UserResponseDTO(
        UUID id,
        String email,
        String username,
		Set<String> roles
) {
	public static UserResponseDTO from(User user) {
		var roles = user.getRoles().stream()
				.map(Role::name)
				.collect(Collectors.toSet());

		return new UserResponseDTO(
				user.getId(),
				user.getEmail(),
				user.getUsername(),
				roles);
	}
}
