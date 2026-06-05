package io.audita.api.controller;

import io.audita.api.dto.request.*;
import io.audita.api.dto.response.AuthResponse;
import io.audita.application.port.AuthPort;
import io.audita.application.port.AuthPort.LoginResult;
import io.audita.infrastructure.tenant.TenantContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final String MESSAGE_KEY = "message";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final AuthPort authService;

    @Value("${audita.jwt.expiry-seconds:900}")
    private long jwtExpirySeconds;

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    @Value("${audita.auth.cookie-path:/api/v1/auth}")
    private String refreshCookiePath;

    @Value("${audita.auth.cookie-secure:true}")
    private boolean refreshCookieSecure;

    @Value("${audita.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    public AuthController(AuthPort authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Tenant-Slug", required = false) String tenantSlug,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {

        String resolvedTenantSlug = resolveTenantSlug(tenantSlug);
        LoginResult result;
        if (resolvedTenantSlug == null) {
            String clientIp = resolveClientIp(servletRequest);
            result = authService.loginSuperAdmin(request.email(), request.password(), clientIp);
        } else {
            String clientIp = resolveClientIp(servletRequest);
            String userAgent = servletRequest.getHeader(USER_AGENT_HEADER);
            result = authService.loginTenantUser(
                    request.email(),
                    request.password(),
                    resolvedTenantSlug,
                    clientIp,
                    userAgent);
        }

        setRefreshCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(toAuthResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawToken = extractRefreshCookie(request);
        if (rawToken == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        if (!hasTenantContext(request)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        LoginResult result = authService.refreshToken(rawToken, clientIp, userAgent);
        setRefreshCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(toAuthResponse(result));
    }

    @PostMapping("/session")
    public ResponseEntity<AuthResponse> session(HttpServletRequest request) {

        String rawToken = extractRefreshCookie(request);
        if (rawToken == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        if (!hasTenantContext(request)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        LoginResult result = authService.restoreSession(rawToken, clientIp, userAgent);
        return ResponseEntity.ok(toAuthResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        if (extractRefreshCookie(request) != null && !hasTenantContext(request)) {
            clearRefreshCookie(response);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        authService.logout(extractRefreshCookie(request));
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            @RequestHeader(value = "X-Tenant-Slug") String tenantSlug) {
        authService.forgotPassword(request.email(), tenantSlug);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY,
                "If that email is registered, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Password reset successfully."));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<Map<String, String>> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request) {
        authService.acceptInvite(request.token(), request.password());
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Account activated. You can now sign in."));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        if (rawToken == null)
            return;
        Cookie cookie = new Cookie(REFRESH_COOKIE, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshCookieSecure);
        cookie.setPath(refreshCookiePath);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge((int) (refreshExpiryDays * 24 * 60 * 60));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshCookieSecure);
        cookie.setPath(refreshCookiePath);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean hasTenantContext(HttpServletRequest request) {
        return resolveTenantSlug(request.getHeader("X-Tenant-Slug")) != null;
    }

    private String resolveTenantSlug(String headerTenantSlug) {
        String contextTenantSlug = TenantContext.getCurrentTenant();
        if (contextTenantSlug != null && !contextTenantSlug.isBlank()) {
            return contextTenantSlug;
        }
        if (headerTenantSlug == null || headerTenantSlug.isBlank()) {
            return null;
        }
        return headerTenantSlug.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse toAuthResponse(LoginResult result) {
        return AuthResponse.of(result.accessToken(), jwtExpirySeconds,
                result.userId(), result.email(), result.fullName(),
                result.role(), result.tenantSlug());
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (!trustForwardedHeaders) {
            return request.getRemoteAddr();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; take the first (client)
            // IP
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
