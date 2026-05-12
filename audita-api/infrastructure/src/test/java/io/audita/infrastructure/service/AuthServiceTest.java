package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.PasswordResetTokenEntity;
import io.audita.infrastructure.persistence.entity.RefreshTokenEntity;
import io.audita.infrastructure.persistence.entity.SuperAdminEntity;
import io.audita.infrastructure.persistence.entity.TenantAllowedDomainEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.InviteTokenRepository;
import io.audita.infrastructure.persistence.repository.PasswordResetTokenRepository;
import io.audita.infrastructure.persistence.repository.RefreshTokenRepository;
import io.audita.infrastructure.persistence.repository.SuperAdminRepository;
import io.audita.infrastructure.persistence.repository.TenantAllowedDomainRepository;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        UserRepository userRepository;
        @Mock
        SuperAdminRepository superAdminRepository;
        @Mock
        RefreshTokenRepository refreshTokenRepository;
        @Mock
        PasswordResetTokenRepository passwordResetTokenRepository;
        @Mock
        InviteTokenRepository inviteTokenRepository;
        @Mock
        TenantRepository tenantRepository;
        @Mock
        TenantAllowedDomainRepository allowedDomainRepository;
        @Mock
        JwtService jwtService;
        @Mock
        EmailService emailService;

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

        @InjectMocks
        AuthService authService;

        private static final String TENANT = "acme";
        private static final String CLIENT_IP = "127.0.0.1";
        private static final String USER_AGENT = "JUnit-Agent/1.0";

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
                ReflectionTestUtils.setField(authService, "jwtExpirySeconds", 900L);
                ReflectionTestUtils.setField(authService, "refreshExpiryDays", 7L);
                TenantContext.setCurrentTenant(TENANT);
        }

        @AfterEach
        void tearDown() {
                TenantContext.clear();
        }

        @Test
        void login_success_returns_tokens() {
                UserEntity user = activeUser("alice@acme.com", "StrongPass1!A");
                when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
                when(allowedDomainRepository.findByTenantSlug(TENANT)).thenReturn(List.of());
                when(jwtService.issue(any(), any(), any(), any())).thenReturn("access-token");
                when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                AuthService.LoginResult result = authService.loginTenantUser(
                                "alice@acme.com", "StrongPass1!A", TENANT, CLIENT_IP, USER_AGENT);

                assertThat(result.accessToken()).isEqualTo("access-token");
                assertThat(result.rawRefreshToken()).isNotBlank();
        }

        @Test
        void login_domain_not_in_whitelist_throws() {
                UserEntity user = activeUser("alice@other.com", "StrongPass1!A");
                when(userRepository.findByEmail("alice@other.com")).thenReturn(Optional.of(user));
                when(allowedDomainRepository.findByTenantSlug(TENANT))
                                .thenReturn(List.of(new TenantAllowedDomainEntity(null, "acme.com")));

                DomainNotPermittedException ex = assertThrows(DomainNotPermittedException.class,
                                () -> authService.loginTenantUser(
                                                "alice@other.com", "StrongPass1!A", TENANT, CLIENT_IP, USER_AGENT));
                assertThat(ex.getErrorCode()).isEqualTo("DOMAIN_NOT_PERMITTED");
        }

        @Test
        void refresh_token_rotates_and_enforces_context_binding() {
                UserEntity user = activeUser("alice@acme.com", "StrongPass1!A");
                String raw = "raw-token";
                RefreshTokenEntity stored = new RefreshTokenEntity(
                                user,
                                AuthService.sha256(raw),
                                OffsetDateTime.now().plusDays(7),
                                AuthService.sha256(USER_AGENT),
                                AuthService.sha256(CLIENT_IP));

                when(refreshTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                                .thenReturn(Optional.of(stored));
                when(jwtService.issue(any(), any(), any(), any())).thenReturn("new-access");
                when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                AuthService.LoginResult result = authService.refreshToken(raw, CLIENT_IP, USER_AGENT);

                assertThat(result.accessToken()).isEqualTo("new-access");
                assertThat(stored.isRevoked()).isTrue();
        }

        @Test
        void restore_session_returns_access_token_without_revoking_refresh_token() {
                UserEntity user = activeUser("alice@acme.com", "StrongPass1!A");
                String raw = "raw-token";
                RefreshTokenEntity stored = new RefreshTokenEntity(
                                user,
                                AuthService.sha256(raw),
                                OffsetDateTime.now().plusDays(7),
                                AuthService.sha256(USER_AGENT),
                                AuthService.sha256(CLIENT_IP));

                when(refreshTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                                .thenReturn(Optional.of(stored));
                when(jwtService.issue(any(), any(), any(), any())).thenReturn("restored-access");

                AuthService.LoginResult result = authService.restoreSession(raw, CLIENT_IP, USER_AGENT);

                assertThat(result.accessToken()).isEqualTo("restored-access");
                assertThat(result.rawRefreshToken()).isNull();
                assertThat(stored.isRevoked()).isFalse();
        }

        @Test
        void refresh_with_context_mismatch_throws() {
                UserEntity user = activeUser("alice@acme.com", "StrongPass1!A");
                String raw = "raw-token";
                RefreshTokenEntity stored = new RefreshTokenEntity(
                                user,
                                AuthService.sha256(raw),
                                OffsetDateTime.now().plusDays(7),
                                AuthService.sha256(USER_AGENT),
                                AuthService.sha256(CLIENT_IP));

                when(refreshTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                                .thenReturn(Optional.of(stored));

                DomainNotPermittedException ex = assertThrows(DomainNotPermittedException.class,
                                () -> authService.refreshToken(raw, "10.0.0.1", USER_AGENT));
                assertThat(ex).hasMessageContaining("invalid");
        }

        @Test
        void reset_password_requires_strong_password() {
                String raw = "reset-token";

                DomainNotPermittedException ex = assertThrows(DomainNotPermittedException.class,
                                () -> authService.resetPassword(raw, "weak"));
                assertThat(ex.getErrorCode()).isEqualTo("WEAK_PASSWORD");
        }

        @Test
        void reset_password_updates_hash_and_marks_token_used() {
                UserEntity user = activeUser("alice@acme.com", "StrongPass1!A");
                String raw = "reset-token";
                PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                                user, AuthService.sha256(raw), OffsetDateTime.now().plusHours(1));

                when(passwordResetTokenRepository.findByTokenHash(AuthService.sha256(raw)))
                                .thenReturn(Optional.of(token));
                when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
                when(passwordResetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
                doNothing().when(refreshTokenRepository).revokeAllForUser(any());

                authService.resetPassword(raw, "NewStrong1!Aa");

                assertThat(passwordEncoder.matches("NewStrong1!Aa", user.getPasswordHash())).isTrue();
                assertThat(token.isUsed()).isTrue();
        }

        @Test
        void bootstrap_requires_strong_password_and_is_single_use() {
                when(superAdminRepository.count()).thenReturn(0L);
                when(superAdminRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                authService.bootstrap("Admin", "admin@audita.io", "StrongPass1!A");

                verify(superAdminRepository).save(argThat(
                                (SuperAdminEntity sa) -> sa.getEmail().equals("admin@audita.io")
                                                && sa.getFullName().equals("Admin")));

                when(superAdminRepository.count()).thenReturn(1L);
                DomainNotPermittedException ex = assertThrows(DomainNotPermittedException.class,
                                () -> authService.bootstrap("Admin", "admin@audita.io", "StrongPass1!A"));
                assertThat(ex.getErrorCode()).isEqualTo("ALREADY_BOOTSTRAPPED");
        }

        @Test
        void logout_null_token_is_no_op() {
                authService.logout(null);
                verifyNoInteractions(refreshTokenRepository);
        }

        private UserEntity activeUser(String email, String rawPassword) {
                UserEntity user = new UserEntity(email, "Test User");
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setStatus(UserStatus.ACTIVE);
                return user;
        }
}
