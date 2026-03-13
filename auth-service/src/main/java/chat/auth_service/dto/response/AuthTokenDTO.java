package chat.auth_service.dto.response;

public record AuthTokenDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
        public static AuthTokenDTO of(String access, String refresh, long expiresIn) {
                return new AuthTokenDTO(access, refresh, "Bearer", expiresIn);
        }
}
