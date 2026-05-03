package io.audita.infrastructure.service;

import io.audita.application.port.OnboardingPort;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.OAuthProvider;
import io.audita.domain.model.TenantStatus;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.AesEncryptionService;
import io.audita.infrastructure.tenant.FlywayTenantMigrator;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Handles tenant lifecycle: provisioning (atomic), CRUD, domain whitelist, SSO config.
 *
 * Provisioning is the critical path: schema creation + Flyway + Admin user + invite
 * must all succeed or none of it persists (Outbox pattern simplified via single TX).
 */
@Service
@Transactional
public class TenantService implements OnboardingPort {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final TenantAllowedDomainRepository allowedDomainRepository;
    private final TenantSsoConfigRepository ssoConfigRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final FlywayTenantMigrator flywayTenantMigrator;
    private final EmailService emailService;
    private final AesEncryptionService aesEncryptionService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public TenantService(TenantRepository tenantRepository,
                         TenantAllowedDomainRepository allowedDomainRepository,
                         TenantSsoConfigRepository ssoConfigRepository,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         InviteTokenRepository inviteTokenRepository,
                         FlywayTenantMigrator flywayTenantMigrator,
                         EmailService emailService,
                         AesEncryptionService aesEncryptionService,
                         PasswordEncoder passwordEncoder,
                         PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.allowedDomainRepository = allowedDomainRepository;
        this.ssoConfigRepository = ssoConfigRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.flywayTenantMigrator = flywayTenantMigrator;
        this.emailService = emailService;
        this.aesEncryptionService = aesEncryptionService;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ── List / Get ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public String findFirstTenantSlug() {
        return tenantRepository.findAll().stream()
                .findFirst()
                .map(TenantEntity::getSlug)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<TenantEntity> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public TenantEntity getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Tenant not found."));
    }

    // ── Provision ──────────────────────────────────────────────────────────────

    /**
     * Atomically provisions a new tenant:
     * 1. Validate slug uniqueness
     * 2. Persist TenantEntity (public schema)
     * 3. Run FlywayTenantMigrator — creates tenant schema + seeds roles/permissions
     * 4. Set TenantContext and create the Admin user (PENDING, no password yet)
     * 5. Create InviteToken and dispatch invite email
     *
     * If any step fails the transaction rolls back. Flyway DDL is separate
     * from JPA transaction — schema cleanup is handled by the caller if needed.
     */
    public TenantEntity provision(String name, String slug, String adminEmail, String adminFullName) {
        String normalisedSlug = slug.toLowerCase().replaceAll("[^a-z0-9-]", "-");

        if (tenantRepository.existsBySlug(normalisedSlug)) {
            throw new DomainNotPermittedException("SLUG_TAKEN",
                    "The slug '" + normalisedSlug + "' is already in use.");
        }

        // 1. Persist tenant record in public schema
        TenantEntity tenant = tenantRepository.save(new TenantEntity(name, normalisedSlug));
        log.info("Tenant created: id={} slug={}", tenant.getId(), normalisedSlug);

        // 2. Run per-tenant Flyway migrations (creates schema + seeds roles/permissions)
        // This runs outside the JPA transaction against the DataSource directly.
        // On failure, the JPA transaction will roll back the tenant record.
        flywayTenantMigrator.migrate(normalisedSlug);

        // 3. Switch context to the new tenant schema to create Admin user
        TenantContext.setCurrentTenant(normalisedSlug);
        try {
            transactionTemplate.executeWithoutResult(status -> {
            RoleEntity adminRole = roleRepository.findByName("Admin")
                .orElseThrow(() -> new IllegalStateException(
                    "Admin role not found after migration for tenant: " + normalisedSlug));

            UserEntity adminUser = new UserEntity(adminEmail, adminFullName);
            adminUser.setRole(adminRole);
            adminUser.setStatus(UserStatus.PENDING);
            userRepository.save(adminUser);

            // 4. Issue invite token (48-hour expiry)
            String rawToken = generateSecureToken();
            String tokenHash = AuthService.sha256(rawToken);
            InviteTokenEntity invite = new InviteTokenEntity(
                adminUser, tokenHash, OffsetDateTime.now().plusHours(48));
            inviteTokenRepository.save(invite);

            // 5. Send invite email asynchronously — failure logged but never rethrows
            emailService.sendInviteEmail(adminEmail, adminFullName, rawToken, name);

            log.info("Admin invited for tenant={} email={}", normalisedSlug, adminEmail);
            });
        } finally {
            TenantContext.clear();
        }

        return tenant;
    }

    // ── Single-tenant first-run setup ──────────────────────────────────────────

    /**
     * Single-tenant first-run setup.
     * Creates the tenant and the Admin user with a password set directly (no invite).
     * Guard: fails if any tenant already exists.
     */
    @Override
    public void setupSingleTenant(String orgName, String slug, String adminFullName,
                                  String adminEmail, String rawPassword) {
        if (tenantRepository.count() > 0) {
            throw new DomainNotPermittedException("ALREADY_SETUP",
                    "Organisation has already been set up.");
        }

        String normalisedSlug = slug.toLowerCase().replaceAll("[^a-z0-9-]", "-");

        // 1. Persist tenant record in public schema
        TenantEntity tenant = tenantRepository.save(new TenantEntity(orgName, normalisedSlug));
        log.info("Single-tenant setup: tenant created id={} slug={}", tenant.getId(), normalisedSlug);

        // 2. Run per-tenant Flyway migrations
        flywayTenantMigrator.migrate(normalisedSlug);

        // 3. Create Admin user with password directly (no invite)
        TenantContext.setCurrentTenant(normalisedSlug);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                RoleEntity adminRole = roleRepository.findByName("Admin")
                        .orElseThrow(() -> new IllegalStateException(
                                "Admin role not found after migration for tenant: " + normalisedSlug));

                UserEntity adminUser = new UserEntity(adminEmail, adminFullName);
                adminUser.setRole(adminRole);
                adminUser.setPasswordHash(passwordEncoder.encode(rawPassword));
                adminUser.setStatus(UserStatus.ACTIVE);
                userRepository.save(adminUser);

                log.info("Admin user created for tenant={} email={}", normalisedSlug, adminEmail);
            });
        } finally {
            TenantContext.clear();
        }

    }

    // ── Update ─────────────────────────────────────────────────────────────────

    public TenantEntity updateTenant(UUID id, String name, TenantStatus status) {
        TenantEntity tenant = getTenant(id);
        if (name != null && !name.isBlank()) {
            tenant.setName(name);
        }
        if (status != null) {
            tenant.setStatus(status);
        }
        return tenantRepository.save(tenant);
    }

    public void deleteTenant(UUID id) {
        TenantEntity tenant = getTenant(id);
        tenantRepository.delete(tenant);
        log.info("Tenant deleted: id={} slug={}", id, tenant.getSlug());
    }

    // ── Domain Whitelist ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TenantAllowedDomainEntity> listDomains(UUID tenantId) {
        TenantEntity tenant = getTenant(tenantId);
        return allowedDomainRepository.findByTenantSlug(tenant.getSlug());
    }

    public TenantAllowedDomainEntity addDomain(UUID tenantId, String domain) {
        TenantEntity tenant = getTenant(tenantId);
        String normalisedDomain = domain.toLowerCase().trim();
        TenantAllowedDomainEntity entity = new TenantAllowedDomainEntity(tenant, normalisedDomain);
        return allowedDomainRepository.save(entity);
    }

    public void removeDomain(UUID domainId) {
        if (!allowedDomainRepository.existsById(domainId)) {
            throw new DomainNotPermittedException("NOT_FOUND", "Domain not found.");
        }
        allowedDomainRepository.deleteById(domainId);
    }

    // ── SSO Config ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TenantSsoConfigEntity> listSsoConfigs(UUID tenantId) {
        TenantEntity tenant = getTenant(tenantId);
        return ssoConfigRepository.findByTenantId(tenant.getId());
    }

    public TenantSsoConfigEntity upsertSsoConfig(UUID tenantId, OAuthProvider provider,
                                                  String clientId, String rawClientSecret,
                                                  String msTenantId) {
        TenantEntity tenant = getTenant(tenantId);
        String encryptedSecret = aesEncryptionService.encrypt(rawClientSecret);

        // Upsert: update existing or create new
        TenantSsoConfigEntity config = ssoConfigRepository
                .findByTenantIdAndProvider(tenant.getId(), provider)
                .orElse(new TenantSsoConfigEntity(tenant, provider, clientId, encryptedSecret, msTenantId));

        config.setClientId(clientId);
        config.setClientSecret(encryptedSecret);
        config.setMsTenantId(msTenantId);
        config.setEnabled(true);
        return ssoConfigRepository.save(config);
    }

    public void deleteSsoConfig(UUID configId) {
        if (!ssoConfigRepository.existsById(configId)) {
            throw new DomainNotPermittedException("NOT_FOUND", "SSO config not found.");
        }
        ssoConfigRepository.deleteById(configId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
