package io.audita.api.dto.response;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        String fullName,
        String role,
        String tenantSlug
) {
    public static AuthResponse of(String accessToken, long expiresIn,
                                   UUID userId, String email, String fullName,
                                   String role, String tenantSlug) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, userId, email, fullName, role, tenantSlug);
    }
}
