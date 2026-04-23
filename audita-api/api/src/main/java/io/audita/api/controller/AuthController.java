package io.audita.api.controller;

import io.audita.api.dto.request.*;
import io.audita.api.dto.response.AuthResponse;
import io.audita.infrastructure.service.AuthService;
import io.audita.infrastructure.service.AuthService.LoginResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";

    private final AuthService authService;

    @Value("${audita.jwt.expiry-seconds:900}")
    private long jwtExpirySeconds;

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    // ── Local login ───────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Tenant-Slug", required = false) String tenantSlug,
            HttpServletResponse response) {

        LoginResult result;
        if (tenantSlug == null || tenantSlug.isBlank()) {
            // Attempt Super Admin login
            result = authService.loginSuperAdmin(request.email(), request.password());
        } else {
            result = authService.loginTenantUser(request.email(), request.password(), tenantSlug);
        }

        setRefreshCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(toAuthResponse(result));
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawToken = extractRefreshCookie(request);
        if (rawToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResult result = authService.refreshToken(rawToken);
        setRefreshCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(toAuthResponse(result));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawToken = extractRefreshCookie(request);
        authService.logout(rawToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    // ── Forgot / Reset password ───────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        // Always 200 — prevents email enumeration
        return ResponseEntity.ok(Map.of("message",
                "If that email is registered, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    // ── Accept invite ─────────────────────────────────────────────────────────

    @PostMapping("/accept-invite")  // Mapped under /api/v1/users/accept-invite in UserController too
    public ResponseEntity<Map<String, String>> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request) {
        authService.acceptInvite(request.token(), request.fullName(), request.password());
        return ResponseEntity.ok(Map.of("message", "Account activated. You can now sign in."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        if (rawToken == null) return;
        Cookie cookie = new Cookie(REFRESH_COOKIE, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);   // HTTPS only in production
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge((int) (refreshExpiryDays * 24 * 60 * 60));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private AuthResponse toAuthResponse(LoginResult result) {
        return AuthResponse.of(
                result.accessToken(), jwtExpirySeconds,
                result.userId(), result.email(), result.fullName(),
                result.role(), result.tenantSlug());
    }
}
