package io.audita.application.port;

import java.util.UUID;

public interface AuthPort {

    LoginResult loginTenantUser(String email,
            String rawPassword,
            String tenantSlug,
            String clientIp,
            String userAgent);

    LoginResult loginSuperAdmin(String email, String rawPassword, String clientIp);

    LoginResult restoreSession(String rawRefreshToken, String clientIp, String userAgent);

    LoginResult refreshToken(String rawRefreshToken, String clientIp, String userAgent);

    void logout(String rawRefreshToken);

    void forgotPassword(String email, String tenantSlug);

    void resetPassword(String rawToken, String newPassword);

    void acceptInvite(String rawToken, String password);

    void bootstrap(String fullName, String email, String rawPassword);

    boolean isOnboardingCompleted();

    record LoginResult(
            String accessToken,
            String rawRefreshToken,
            UUID userId,
            String email,
            String fullName,
            String role,
            String tenantSlug) {
    }
}