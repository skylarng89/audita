package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock SuperAdminRepository superAdminRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock InviteTokenRepository inviteTokenRepository;
    @Mock TenantRepository tenantRepository;
    @Mock TenantAllowedDomainRepository allowedDomainRepository;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;

    // Use real BCrypt so password checks behave correctly
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @InjectMocks
    AuthService authService;

    private static final String TENANT = "acme";
    private static final String CLIENT_IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        // Inject real passwordEncoder (Mockito @InjectMocks picks the field by type)
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authService, "jwtExpirySeconds", 900L);
        ReflectionTestUtils.setField(authService, "refreshExpiryDays", 7L);
        TenantContext.setCurrentTenant(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    void login_success_returns_tokens() {
        UserEntity user = activeUser("alice@acme.com", passwordEncoder.encode("Password1!"));
        when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
        when(allowedDomainRepository.findByTenantSlug(TENANT)).thenReturn(List.of());
        when(jwtService.issue(any(), any(), any(), any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthService.LoginResult result = authService.loginTenantUser("alice@acme.com", "Password1!", TENANT, CLIENT_IP);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(result.email()).isEqualTo("alice@acme.com");
    }

    @Test
    void login_wrong_password_throws() {
        UserEntity user = activeUser("alice@acme.com", passwordEncoder.encode("correct"));
        when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.loginTenantUser("alice@acme.com", "wrong", TENANT, CLIENT_IP))
                .isInstanceOf(DomainNotPermittedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_unknown_user_throws() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.loginTenantUser("nobody@acme.com", "pw", TENANT, CLIENT_IP))
                .isInstanceOf(DomainNotPermittedException.class);
    }

    @Test
    void login_suspended_user_throws() {
        UserEntity user = activeUser("alice@acme.com", passwordEncoder.encode("pw"));
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.loginTenantUser("alice@acme.com", "pw", TENANT, CLIENT_IP))
                .isInstanceOf(DomainNotPermittedException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    void login_domain_not_in_whitelist_throws() {
        UserEntity user = activeUser("alice@other.com", passwordEncoder.encode("pw"));
        when(userRepository.findByEmail("alice@other.com")).thenReturn(Optional.of(user));

        TenantAllowedDomainEntity allowed = allowedDomain("acme.com");
        when(allowedDomainRepository.findByTenantSlug(TENANT)).thenReturn(List.of(allowed));

        assertThatThrownBy(() ->
                authService.loginTenantUser("alice@other.com", "pw", TENANT, CLIENT_IP))
                .isInstanceOf(DomainNotPermittedException.class)
                .extracting(e -> ((DomainNotPermittedException) e).getErrorCode())
                .isEqualTo("DOMAIN_NOT_PERMITTED");
    }

    @Test
    void login_domain_in_whitelist_succeeds() {
        UserEntity user = activeUser("alice@acme.com", passwordEncoder.encode("pw"));
        when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
        when(allowedDomainRepository.findByTenantSlug(TENANT)).thenReturn(List.of(allowedDomain("acme.com")));
        when(jwtService.issue(any(), any(), any(), any())).thenReturn("tok");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthService.LoginResult result = authService.loginTenantUser("alice@acme.com", "pw", TENANT, CLIENT_IP);
        assertThat(result.accessToken()).isEqualTo("tok");
    }

    // ── rate limiting ──────────────────────────────────────────────────────────

    @Test
    void login_rate_limit_blocks_after_5_attempts() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // First 5 attempts throw INVALID_CREDENTIALS (not rate-limited yet)
        for (int i = 0; i < 5; i++) {
            try { authService.loginTenantUser("x@acme.com", "pw", TENANT, "1.2.3.4"); }
            catch (DomainNotPermittedException e) {
                assertThat(e.getMessage()).doesNotContain("TOO_MANY_ATTEMPTS");
            }
        }
        // 6th attempt must be blocked by rate limiter
        assertThatThrownBy(() ->
                authService.loginTenantUser("x@acme.com", "pw", TENANT, "1.2.3.4"))
                .isInstanceOf(DomainNotPermittedException.class)
                .extracting(e -> ((DomainNotPermittedException) e).getErrorCode())
                .isEqualTo("TOO_MANY_ATTEMPTS");
    }

    @Test
    void forgot_password_rate_limit_blocks_after_3_attempts() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            authService.forgotPassword("flood@acme.com"); // always succeeds (enumeration protection)
        }
        assertThatThrownBy(() -> authService.forgotPassword("flood@acme.com"))
                .isInstanceOf(DomainNotPermittedException.class)
                .extracting(e -> ((DomainNotPermittedException) e).getErrorCode())
                .isEqualTo("TOO_MANY_ATTEMPTS");
    }

    // ── refresh token ──────────────────────────────────────────────────────────

    @Test
    void refresh_token_rotates_and_returns_new_tokens() {
        UserEntity user = activeUser("alice@acme.com", "hash");
        RefreshTokenEntity stored = validRefreshToken(user);
        String raw = "raw-token";

        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                .thenReturn(Optional.of(stored));
        when(jwtService.issue(any(), any(), any(), any())).thenReturn("new-access");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthService.LoginResult result = authService.refreshToken(raw);

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue(); // old token revoked
    }

    @Test
    void refresh_with_revoked_token_throws() {
        UserEntity user = activeUser("alice@acme.com", "hash");
        RefreshTokenEntity stored = validRefreshToken(user);
        stored.setRevoked(true);
        String raw = "raw-token";

        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refreshToken(raw))
                .isInstanceOf(DomainNotPermittedException.class);
    }

    // ── reset password ─────────────────────────────────────────────────────────

    @Test
    void reset_password_updates_hash_and_marks_token_used() {
        UserEntity user = activeUser("alice@acme.com", "old-hash");
        String raw = "reset-raw";
        PasswordResetTokenEntity token = validResetToken(user, raw);

        when(passwordResetTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                .thenReturn(Optional.of(token));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwordResetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(refreshTokenRepository).revokeAllForUser(any());

        authService.resetPassword(raw, "NewPass1!");

        assertThat(passwordEncoder.matches("NewPass1!", user.getPasswordHash())).isTrue();
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void reset_password_expired_token_throws() {
        UserEntity user = activeUser("alice@acme.com", "hash");
        String raw = "expired";
        PasswordResetTokenEntity token = expiredResetToken(user, raw);

        when(passwordResetTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(raw, "new"))
                .isInstanceOf(DomainNotPermittedException.class)
                .hasMessageContaining("expired");
    }

    // ── bootstrap ─────────────────────────────────────────────────────────────

    @Test
    void bootstrap_creates_super_admin_when_none_exists() {
        when(superAdminRepository.count()).thenReturn(0L);
        when(superAdminRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.bootstrap("Admin", "admin@audita.io", "pass");

        verify(superAdminRepository).save(argThat(sa ->
                sa.getEmail().equals("admin@audita.io") && sa.getFullName().equals("Admin")));
    }

    @Test
    void bootstrap_fails_if_already_bootstrapped() {
        when(superAdminRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> authService.bootstrap("Admin", "admin@audita.io", "pass"))
                .isInstanceOf(DomainNotPermittedException.class)
                .extracting(e -> ((DomainNotPermittedException) e).getErrorCode())
                .isEqualTo("ALREADY_BOOTSTRAPPED");
    }

    // ── logout ─────────────────────────────────────────────────────────────────

    @Test
    void logout_revokes_refresh_token() {
        UserEntity user = activeUser("alice@acme.com", "hash");
        String raw = "logout-token";
        RefreshTokenEntity stored = validRefreshToken(user);

        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                .thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.logout(raw);

        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void logout_null_token_is_no_op() {
        authService.logout(null);
        verifyNoInteractions(refreshTokenRepository);
    }

    // ── sha256 ─────────────────────────────────────────────────────────────────

    @Test
    void sha256_is_deterministic() {
        assertThat(AuthService.sha256("abc")).isEqualTo(AuthService.sha256("abc"));
    }

    @Test
    void sha256_different_inputs_produce_different_hashes() {
        assertThat(AuthService.sha256("a")).isNotEqualTo(AuthService.sha256("b"));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private UserEntity activeUser(String email, String passwordHash) {
        UserEntity u = new UserEntity(email, "Test User");
        u.setPasswordHash(passwordHash);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private TenantAllowedDomainEntity allowedDomain(String domain) {
        return new TenantAllowedDomainEntity(null, domain);
    }

    private RefreshTokenEntity validRefreshToken(UserEntity user) {
        return new RefreshTokenEntity(user, "hash", OffsetDateTime.now().plusDays(7));
    }

    private PasswordResetTokenEntity validResetToken(UserEntity user, String raw) {
        return new PasswordResetTokenEntity(user, AuthService.sha256(raw), OffsetDateTime.now().plusHours(1));
    }

    private PasswordResetTokenEntity expiredResetToken(UserEntity user, String raw) {
        return new PasswordResetTokenEntity(user, AuthService.sha256(raw), OffsetDateTime.now().minusMinutes(1));
    }
}
