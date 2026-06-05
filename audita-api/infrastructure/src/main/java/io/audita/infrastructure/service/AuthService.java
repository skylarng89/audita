package io.audita.infrastructure.service;

import io.audita.application.port.AuthPort;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.security.RoleHierarchy;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@Transactional
public class AuthService implements AuthPort {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_TOKEN_CODE = "INVALID_TOKEN";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SuperAdminRepository superAdminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final TenantRepository tenantRepository;
    private final TenantAllowedDomainRepository allowedDomainRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // In-memory rate limiting: key → list of request timestamps
    // login: 5 attempts per 15 minutes per IP+email
    // forgot-password: 3 attempts per hour per email
    private final ConcurrentHashMap<String, LinkedList<Instant>> rateBuckets = new ConcurrentHashMap<>();
    private final AtomicInteger rateLimitChecks = new AtomicInteger(0);
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$");

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    public AuthService(UserRepository userRepository,
            SuperAdminRepository superAdminRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            InviteTokenRepository inviteTokenRepository,
            TenantRepository tenantRepository,
            TenantAllowedDomainRepository allowedDomainRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.superAdminRepository = superAdminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.tenantRepository = tenantRepository;
        this.allowedDomainRepository = allowedDomainRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    @Override
    public LoginResult loginTenantUser(String email,
            String rawPassword,
            String tenantSlug,
            String clientIp,
            String userAgent) {
        String normalizedEmail = email.trim().toLowerCase();
        String rateLimitKey = "login:" + clientIp + ":" + normalizedEmail;
        checkRateLimit(rateLimitKey, 5, Duration.ofMinutes(15), "TOO_MANY_ATTEMPTS",
                "Too many login attempts. Please try again in 15 minutes.");

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    recordFailedAttempt(rateLimitKey);
                    return new DomainNotPermittedException(
                            "INVALID_CREDENTIALS", "Invalid email or password.");
                });

        if (user.getStatus() == UserStatus.PENDING) {
            recordFailedAttempt(rateLimitKey);
            throw new DomainNotPermittedException("ACCOUNT_PENDING",
                    "Your account is not yet active. Please accept your invite link first.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            recordFailedAttempt(rateLimitKey);
            throw new DomainNotPermittedException("ACCOUNT_SUSPENDED",
                    "Your account has been suspended. Contact your administrator.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            recordFailedAttempt(rateLimitKey);
            throw new DomainNotPermittedException("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        checkDomainWhitelist(normalizedEmail, tenantSlug);

        clearRateLimit(rateLimitKey);
        return issueTokensForUser(user, tenantSlug, clientIp, userAgent);
    }

    @Override
    public LoginResult loginSuperAdmin(String email, String rawPassword) {
        SuperAdminEntity sa = superAdminRepository.findByEmail(email)
                .orElseThrow(() -> new DomainNotPermittedException(
                        "INVALID_CREDENTIALS", "Invalid email or password."));

        if (!passwordEncoder.matches(rawPassword, sa.getPasswordHash())) {
            throw new DomainNotPermittedException("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        String accessToken = jwtService.issue(sa.getId(), sa.getEmail(), "SUPER_ADMIN", null);
        return new LoginResult(accessToken, null, sa.getId(), sa.getEmail(),
                sa.getFullName(), "SUPER_ADMIN", null);
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    @Override
    public LoginResult restoreSession(String rawRefreshToken, String clientIp, String userAgent) {
        RefreshTokenEntity token = requireValidRefreshToken(rawRefreshToken, clientIp, userAgent);
        String tenantSlug = TenantContext.getCurrentTenant();
        return issueAccessTokenForExistingSession(token.getUser(), tenantSlug);
    }

    @Override
    public LoginResult refreshToken(String rawRefreshToken, String clientIp, String userAgent) {
        RefreshTokenEntity token = requireValidRefreshToken(rawRefreshToken, clientIp, userAgent);

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        // TenantContext is already set by TenantResolutionFilter from X-Tenant-Slug
        // header
        String tenantSlug = TenantContext.getCurrentTenant();
        return issueTokensForUser(token.getUser(), tenantSlug, clientIp, userAgent);
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    @Override
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null)
            return;
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    // ── Forgot / Reset password ─────────────────────────────────────────────────

    @Override
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        String rateLimitKey = "forgot:" + normalizedEmail;
        checkRateLimit(rateLimitKey, 3, Duration.ofHours(1), "TOO_MANY_ATTEMPTS",
                "Too many reset requests. Please try again in an hour.");
        recordFailedAttempt(rateLimitKey);

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            String rawToken = generateSecureToken();
            PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                    user, sha256(rawToken), java.time.OffsetDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
        });
    }

    @Override
    public void resetPassword(String rawToken, String newPassword) {
        validatePasswordStrength(newPassword);
        String hash = sha256(rawToken);
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new DomainNotPermittedException(
                        INVALID_TOKEN_CODE, "Password reset link is invalid or has expired."));

        if (!token.isValid()) {
            throw new DomainNotPermittedException(INVALID_TOKEN_CODE,
                    "Password reset link is invalid or has expired.");
        }

        UserEntity user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset completed for user={}", user.getId());
    }

    // ── Accept invite ───────────────────────────────────────────────────────────

    @Override
    public void acceptInvite(String rawToken, String password) {
        validatePasswordStrength(password);
        String hash = sha256(rawToken);
        InviteTokenEntity token = inviteTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new DomainNotPermittedException(
                        INVALID_TOKEN_CODE, "Invite link is invalid or has expired."));

        if (!token.isValid()) {
            throw new DomainNotPermittedException(INVALID_TOKEN_CODE,
                    "Invite link is invalid or has expired.");
        }

        UserEntity user = token.getUser();
        // fullName already set at invite time — no need to update it here
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        token.setUsed(true);
        inviteTokenRepository.save(token);
        log.info("Invite accepted for user={}", user.getId());
    }

    // ── Bootstrap (first run) ───────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "onboardingStatus", allEntries = true)
    public void bootstrap(String fullName, String email, String rawPassword) {
        if (superAdminRepository.count() > 0) {
            throw new DomainNotPermittedException("ALREADY_BOOTSTRAPPED",
                    "Platform has already been bootstrapped.");
        }
        validatePasswordStrength(rawPassword);
        SuperAdminEntity sa = new SuperAdminEntity(email, passwordEncoder.encode(rawPassword), fullName);
        superAdminRepository.save(sa);
        log.info("Platform bootstrapped. Super Admin created: {}", email);
    }

    @Override
    @Cacheable(value = "onboardingStatus", key = "'completed'")
    public boolean isOnboardingCompleted() {
        // True if either the legacy SUPER_ADMIN bootstrap or the single-tenant setup
        // has run.
        return superAdminRepository.count() > 0 || tenantRepository.count() > 0;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private LoginResult issueTokensForUser(UserEntity user,
            String tenantSlug,
            String clientIp,
            String userAgent) {
        List<RoleEntity> assignedRoles = resolveAssignedRoles(user);
        String role = RoleHierarchy.highestRoleNameOrDefault(assignedRoles, "Requester");
        List<String> roleNames = assignedRoles.stream().map(RoleEntity::getName).toList();
        List<String> permissionCodes = assignedRoles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(permission -> permission.getCode() == null ? "" : permission.getCode())
                .filter(code -> !code.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
        String accessToken = jwtService.issue(user.getId(), user.getEmail(), role, roleNames, permissionCodes,
                tenantSlug);

        String rawRefreshToken = generateSecureToken();
        RefreshTokenEntity refreshToken = new RefreshTokenEntity(
                user,
                sha256(rawRefreshToken),
                java.time.OffsetDateTime.now().plusDays(refreshExpiryDays),
                hashNullable(userAgent),
                hashNullable(clientIp));
        refreshTokenRepository.save(refreshToken);

        return new LoginResult(accessToken, rawRefreshToken, user.getId(),
                user.getEmail(), user.getFullName(), role, tenantSlug);
    }

    private LoginResult issueAccessTokenForExistingSession(UserEntity user, String tenantSlug) {
        List<RoleEntity> assignedRoles = resolveAssignedRoles(user);
        String role = RoleHierarchy.highestRoleNameOrDefault(assignedRoles, "Requester");
        List<String> roleNames = assignedRoles.stream().map(RoleEntity::getName).toList();
        List<String> permissionCodes = assignedRoles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(permission -> permission.getCode() == null ? "" : permission.getCode())
                .filter(code -> !code.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
        String accessToken = jwtService.issue(user.getId(), user.getEmail(), role, roleNames, permissionCodes,
                tenantSlug);
        return new LoginResult(accessToken, null, user.getId(),
                user.getEmail(), user.getFullName(), role, tenantSlug);
    }

    private List<RoleEntity> resolveAssignedRoles(UserEntity user) {
        LinkedHashSet<RoleEntity> roles = new LinkedHashSet<>(user.getRoles());
        if (roles.isEmpty() && user.getRole() != null) {
            roles.add(user.getRole());
        }
        return List.copyOf(roles);
    }

    private RefreshTokenEntity requireValidRefreshToken(String rawRefreshToken,
            String clientIp,
            String userAgent) {
        String hash = sha256(rawRefreshToken);
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new DomainNotPermittedException(
                        INVALID_TOKEN_CODE, "Refresh token is invalid or expired."));

        if (!token.isValid()) {
            throw new DomainNotPermittedException(INVALID_TOKEN_CODE, "Refresh token is invalid or expired.");
        }
        if (!isSessionContextValid(token, clientIp, userAgent)) {
            throw new DomainNotPermittedException(INVALID_TOKEN_CODE, "Refresh token context is invalid.");
        }
        return token;
    }

    /**
     * Domain whitelist check (AUTH-009).
     * If no domains are configured for the tenant, all domains are allowed (open
     * tenant).
     * If domains are configured, the user's email domain must match one of them.
     */
    private void checkDomainWhitelist(String email, String tenantSlug) {
        List<TenantAllowedDomainEntity> allowed = allowedDomainRepository.findByTenantSlug(tenantSlug);
        if (allowed.isEmpty()) {
            return; // No whitelist configured — open tenant
        }
        String emailDomain = email.substring(email.indexOf('@') + 1).toLowerCase();
        boolean permitted = allowed.stream()
                .anyMatch(d -> d.getDomain().equalsIgnoreCase(emailDomain));
        if (!permitted) {
            throw new DomainNotPermittedException("DOMAIN_NOT_PERMITTED",
                    "Your email domain is not authorised for this organisation.");
        }
    }

    /**
     * Sliding-window in-memory rate limiter — check only, does not record the attempt.
     * Call {@link #recordFailedAttempt(String)} on failure, {@link #clearRateLimit(String)} on success.
     * Not distributed — suitable for single-instance deployment; replace with
     * Redis/Bucket4j for HA.
     */
    private void checkRateLimit(String key, int maxRequests, Duration window,
            String errorCode, String message) {
        Instant cutoff = Instant.now().minus(window);
        LinkedList<Instant> bucket = rateBuckets.computeIfAbsent(key, k -> new LinkedList<>());
        synchronized (bucket) {
            bucket.removeIf(t -> t.isBefore(cutoff));
            if (bucket.size() >= maxRequests) {
                throw new DomainNotPermittedException(errorCode, message);
            }
        }

        if (rateLimitChecks.incrementAndGet() % 100 == 0) {
            Instant staleCutoff = Instant.now().minus(Duration.ofHours(2));
            rateBuckets.entrySet().removeIf(entry -> {
                LinkedList<Instant> list = entry.getValue();
                synchronized (list) {
                    list.removeIf(t -> t.isBefore(staleCutoff));
                    return list.isEmpty();
                }
            });
        }
    }

    private void recordFailedAttempt(String key) {
        LinkedList<Instant> bucket = rateBuckets.computeIfAbsent(key, k -> new LinkedList<>());
        synchronized (bucket) {
            bucket.add(Instant.now());
        }
    }

    private void clearRateLimit(String key) {
        rateBuckets.remove(key);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new DomainNotPermittedException(
                    "WEAK_PASSWORD",
                    "Password must be 12+ chars with upper, lower, number, and symbol.");
        }
    }

    private boolean isSessionContextValid(RefreshTokenEntity token, String clientIp, String userAgent) {
        String expectedIpHash = token.getIpHash();
        String expectedUaHash = token.getUserAgentHash();
        return (expectedIpHash == null || expectedIpHash.equals(hashNullable(clientIp)))
                && (expectedUaHash == null || expectedUaHash.equals(hashNullable(userAgent)));
    }

    private String hashNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return sha256(value);
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
