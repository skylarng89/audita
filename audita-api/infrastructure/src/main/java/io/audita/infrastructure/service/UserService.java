package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.RoleHierarchy;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages tenant-scoped user operations: invite, list, update, deactivate, role
 * assignment.
 * All operations run within the tenant schema set by TenantContext.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String ADMIN_ROLE_NAME = "Admin";

    @Value("${audita.invite.expiry-hours:48}")
    private int inviteExpiryHours;

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
        return findUserOrThrow(id);
    }

    // ── Invite ─────────────────────────────────────────────────────────────────

    /**
     * Invites a new user to the current tenant.
     * Creates the user in PENDING status, issues a 48-hour invite token, and sends
     * email.
     * Idempotent on email: if user exists in PENDING state, re-sends the invite.
     */
    public UserEntity inviteUser(String email, String fullName, UUID roleId, List<UUID> roleIds, UUID invitedByUserId) {
        if (userRepository.existsByEmail(email)) {
            throw new DomainNotPermittedException("EMAIL_TAKEN",
                    "A user with this email already exists in this organisation.");
        }

        Set<RoleEntity> assignedRoles = resolveRoles(roleId, roleIds, true);
        RoleEntity effectiveRole = resolveEffectiveRole(assignedRoles);

        UserEntity invitedBy = userRepository.findById(invitedByUserId).orElse(null);

        UserEntity user = new UserEntity(email, fullName);
        user.setRole(effectiveRole);
        user.setRoles(new LinkedHashSet<>(assignedRoles));
        user.setStatus(UserStatus.PENDING);
        user.setInvitedBy(invitedBy);
        userRepository.save(user);

        String rawToken = generateSecureToken();
        String tokenHash = AuthService.sha256(rawToken);
        inviteTokenRepository.save(new InviteTokenEntity(user, tokenHash,
                OffsetDateTime.now().plusHours(inviteExpiryHours)));

        // Resolve tenant name for the email (falls back gracefully if not found)
        String tenantSlug = TenantContext.getCurrentTenant();
        String orgName = tenantRepository.findBySlug(tenantSlug)
                .map(TenantEntity::getName)
                .orElse(tenantSlug);

        emailService.sendInviteEmail(email, fullName, rawToken, orgName, tenantSlug);
        log.info("User invited: email={} roles={} tenant={}",
                email,
                assignedRoles.stream().map(RoleEntity::getName).sorted().toList(),
                tenantSlug);
        return user;
    }

    // ── Update ─────────────────────────────────────────────────────────────────

    public UserEntity updateUser(UUID id, String fullName, UUID roleId, List<UUID> roleIds) {
        UserEntity user = findUserOrThrow(id);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        if (roleId != null || (roleIds != null && !roleIds.isEmpty())) {
            Set<RoleEntity> assignedRoles = resolveRoles(roleId, roleIds, false);
            user.setRoles(new LinkedHashSet<>(assignedRoles));
            user.setRole(resolveEffectiveRole(assignedRoles));
        }

        return userRepository.save(user);
    }

    public void deactivateUser(UUID id, UUID requesterId) {
        if (id.equals(requesterId)) {
            throw new DomainNotPermittedException("SELF_DEACTIVATION",
                    "You cannot deactivate your own account.");
        }

        UserEntity user = findUserOrThrow(id);

        // Prevent removing the last active Admin — organisation would be locked out.
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> ADMIN_ROLE_NAME.equals(role.getName()))
                || (user.getRole() != null && ADMIN_ROLE_NAME.equals(user.getRole().getName()));
        if (isAdmin && userRepository.countDistinctByRoles_NameAndStatus(ADMIN_ROLE_NAME, UserStatus.ACTIVE) <= 1) {
            throw new DomainNotPermittedException("LAST_ADMIN",
                    "Cannot deactivate the last Admin. Assign another Admin first.");
        }

        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("User deactivated: id={} by={}", id, requesterId);
    }

    public void reactivateUser(UUID id) {
        UserEntity user = findUserOrThrow(id);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "User is not suspended and cannot be reactivated.");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User reactivated: id={}", id);
    }

    // ── Invite management ──────────────────────────────────────────────────────

    /**
     * Voids all existing invite tokens for a PENDING user and sends a fresh 48-hour
     * invite.
     */
    public UserEntity resendInvite(UUID userId) {
        UserEntity user = findUserOrThrow(userId);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "Invite can only be resent for users with PENDING status.");
        }

        inviteTokenRepository.deleteAll(inviteTokenRepository.findAllByUser_Id(userId));

        String rawToken = generateSecureToken();
        inviteTokenRepository.save(new InviteTokenEntity(user, AuthService.sha256(rawToken),
                OffsetDateTime.now().plusHours(inviteExpiryHours)));

        String tenantSlug = TenantContext.getCurrentTenant();
        String orgName = tenantRepository.findBySlug(tenantSlug)
                .map(TenantEntity::getName)
                .orElse(tenantSlug);

        emailService.sendInviteEmail(user.getEmail(), user.getFullName(), rawToken, orgName, tenantSlug);
        log.info("Invite resent: userId={} tenant={}", userId, tenantSlug);
        return user;
    }

    /**
     * Cancels a pending invite by removing all invite tokens and deleting the user
     * record.
     * The same email address can be re-invited afterwards.
     */
    public void cancelInvite(UUID userId) {
        UserEntity user = findUserOrThrow(userId);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "Invite can only be cancelled for users with PENDING status.");
        }

        inviteTokenRepository.deleteAll(inviteTokenRepository.findAllByUser_Id(userId));
        userRepository.delete(user);
        log.info("Invite cancelled and user removed: userId={}", userId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserEntity findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException(NOT_FOUND, "User not found."));
    }

    private Set<RoleEntity> resolveRoles(UUID roleId, List<UUID> roleIds, boolean requireAtLeastOne) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        if (roleId != null) {
            ids.add(roleId);
        }
        if (roleIds != null) {
            ids.addAll(roleIds.stream().filter(java.util.Objects::nonNull).toList());
        }

        if (requireAtLeastOne && ids.isEmpty()) {
            throw new DomainNotPermittedException("INVALID_ROLE_ASSIGNMENT", "At least one role is required.");
        }
        if (ids.isEmpty()) {
            return Set.of();
        }

        List<RoleEntity> roles = roleRepository.findAllById(ids);
        if (roles.size() != ids.size()) {
            throw new DomainNotPermittedException(NOT_FOUND, "One or more roles were not found.");
        }
        return new LinkedHashSet<>(roles);
    }

    private RoleEntity resolveEffectiveRole(Set<RoleEntity> roles) {
        String highestRoleName = RoleHierarchy.highestRoleNameOrDefault(roles, "Requester");
        return roles.stream()
                .filter(role -> highestRoleName.equalsIgnoreCase(role.getName()))
                .findFirst()
                .orElseThrow(() -> new DomainNotPermittedException("INVALID_ROLE_ASSIGNMENT",
                        "Could not determine effective role for the user."));
    }
}
