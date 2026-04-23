package io.audita.application.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.NotFoundException;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final SuperAdminRepository superAdminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${audita.jwt.expiry-seconds:900}")
    private long jwtExpirySeconds;

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    // ── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public LoginResult loginTenantUser(String email, String rawPassword, String tenantSlug) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainNotPermittedException("INVALID_CREDENTIALS",
                        "Invalid email or password."));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new DomainNotPermittedException("ACCOUNT_SUSPENDED",
                    "Your account has been suspended. Contact your administrator.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new DomainNotPermittedException("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        return issueTokensForUser(user, tenantSlug);
    }

    @Transactional
    public LoginResult loginSuperAdmin(String email, String rawPassword) {
        SuperAdminEntity sa = superAdminRepository.findByEmail(email)
                .orElseThrow(() -> new DomainNotPermittedException("INVALID_CREDENTIALS",
                        "Invalid email or password."));

        if (!passwordEncoder.matches(rawPassword, sa.getPasswordHash())) {
            throw new DomainNotPermittedException("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        String accessToken = jwtService.issue(sa.getId(), sa.getEmail(), "SUPER_ADMIN", null);
        return new LoginResult(accessToken, null, sa.getId(), sa.getEmail(),
                sa.getFullName(), "SUPER_ADMIN", null);
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    @Transactional
    public LoginResult refreshToken(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new DomainNotPermittedException("INVALID_TOKEN",
                        "Refresh token is invalid or expired."));

        if (!token.isValid()) {
            throw new DomainNotPermittedException("INVALID_TOKEN", "Refresh token is invalid or expired.");
        }

        // Rotate: revoke old, issue new
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return issueTokensForUser(token.getUser(),
                token.getUser().getRole() != null ? extractTenantSlug(token.getUser()) : null);
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null) return;
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    // ── Forgot / Reset Password ───────────────────────────────────────────────

    @Transactional
    public void forgotPassword(String email) {
        // Always return success to prevent email enumeration (AUTH-04)
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = generateSecureToken();
            PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                    user, sha256(rawToken), OffsetDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = sha256(rawToken);
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new DomainNotPermittedException("INVALID_TOKEN",
                        "Password reset link is invalid or has expired."));

        if (!token.isValid()) {
            throw new DomainNotPermittedException("INVALID_TOKEN",
                    "Password reset link is invalid or has expired.");
        }

        UserEntity user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Invalidate all refresh tokens on password change
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset completed for user={}", user.getId());
    }

    // ── Accept Invite ─────────────────────────────────────────────────────────

    @Transactional
    public void acceptInvite(String rawToken, String fullName, String password) {
        String hash = sha256(rawToken);
        InviteTokenEntity token = inviteTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new DomainNotPermittedException("INVALID_TOKEN",
                        "Invite link is invalid or has expired."));

        if (!token.isValid()) {
            throw new DomainNotPermittedException("INVALID_TOKEN",
                    "Invite link is invalid or has expired.");
        }

        UserEntity user = token.getUser();
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        token.setUsed(true);
        inviteTokenRepository.save(token);
        log.info("Invite accepted for user={}", user.getId());
    }

    // ── Bootstrap (first run) ─────────────────────────────────────────────────

    @Transactional
    public void bootstrap(String fullName, String email, String rawPassword) {
        if (superAdminRepository.count() > 0) {
            throw new DomainNotPermittedException("ALREADY_BOOTSTRAPPED",
                    "Platform has already been bootstrapped.");
        }
        SuperAdminEntity sa = new SuperAdminEntity(email, passwordEncoder.encode(rawPassword), fullName);
        superAdminRepository.save(sa);
        log.info("Platform bootstrapped. Super Admin created: {}", email);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private LoginResult issueTokensForUser(UserEntity user, String tenantSlug) {
        String role = user.getRole() != null ? user.getRole().getName() : "Requester";
        String accessToken = jwtService.issue(user.getId(), user.getEmail(), role, tenantSlug);

        String rawRefreshToken = generateSecureToken();
        RefreshTokenEntity refreshToken = new RefreshTokenEntity(
                user, sha256(rawRefreshToken),
                OffsetDateTime.now().plusDays(refreshExpiryDays));
        refreshTokenRepository.save(refreshToken);

        return new LoginResult(accessToken, rawRefreshToken, user.getId(),
                user.getEmail(), user.getFullName(), role, tenantSlug);
    }

    private String extractTenantSlug(UserEntity user) {
        // Tenant slug is resolved from TenantContext at login time — stored in JWT.
        // Here we look it up from the DB for refresh rotation.
        return tenantRepository.findAll().stream()
                .filter(t -> t.getSlug() != null)
                .findFirst()
                .map(TenantEntity::getSlug)
                .orElse(null);
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record LoginResult(
            String accessToken,
            String rawRefreshToken,
            UUID userId,
            String email,
            String fullName,
            String role,
            String tenantSlug
    ) {}
}
