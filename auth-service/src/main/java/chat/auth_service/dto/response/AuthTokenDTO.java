package chat.auth_service.dto.response;

public record AuthTokenDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponseDTO user
) {
        public static AuthTokenDTO of(UserResponseDTO userDTO, String access, String refresh, long expiresIn) {
                return new AuthTokenDTO(access, refresh, "Bearer ", expiresIn, userDTO);
        }
}
