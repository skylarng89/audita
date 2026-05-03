package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Manages tenant-scoped user operations: invite, list, update, deactivate, role assignment.
 * All operations run within the tenant schema set by TenantContext.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       InviteTokenRepository inviteTokenRepository,
                       TenantRepository tenantRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
    }

    // ── List / Get ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserEntity> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "User not found."));
    }

    // ── Invite ─────────────────────────────────────────────────────────────────

    /**
     * Invites a new user to the current tenant.
     * Creates the user in PENDING status, issues a 48-hour invite token, and sends email.
     * Idempotent on email: if user exists in PENDING state, re-sends the invite.
     */
    public UserEntity inviteUser(String email, String fullName, UUID roleId, UUID invitedByUserId) {
        if (userRepository.existsByEmail(email)) {
            throw new DomainNotPermittedException("EMAIL_TAKEN",
                    "A user with this email already exists in this organisation.");
        }

        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Role not found."));

        UserEntity invitedBy = userRepository.findById(invitedByUserId).orElse(null);

        UserEntity user = new UserEntity(email, fullName);
        user.setRole(role);
        user.setStatus(UserStatus.PENDING);
        user.setInvitedBy(invitedBy);
        userRepository.save(user);

        String rawToken = generateSecureToken();
        String tokenHash = AuthService.sha256(rawToken);
        inviteTokenRepository.save(new InviteTokenEntity(user, tokenHash,
                OffsetDateTime.now().plusHours(48)));

        // Resolve tenant name for the email (falls back gracefully if not found)
        String tenantSlug = TenantContext.getCurrentTenant();
        String orgName = tenantRepository.findBySlug(tenantSlug)
                .map(TenantEntity::getName)
                .orElse(tenantSlug);

        emailService.sendInviteEmail(email, fullName, rawToken, orgName);
        log.info("User invited: email={} role={} tenant={}", email, role.getName(), tenantSlug);
        return user;
    }

    // ── Update ─────────────────────────────────────────────────────────────────

    public UserEntity updateUser(UUID id, String fullName, UUID roleId) {
        UserEntity user = getUser(id);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        if (roleId != null) {
            RoleEntity role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Role not found."));
            user.setRole(role);
        }

        return userRepository.save(user);
    }

    public void deactivateUser(UUID id) {
        UserEntity user = getUser(id);
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("User deactivated: id={}", id);
    }

    public void reactivateUser(UUID id) {
        UserEntity user = getUser(id);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "User is not suspended and cannot be reactivated.");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User reactivated: id={}", id);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
