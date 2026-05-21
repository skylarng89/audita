package io.audita.infrastructure.service;

import io.audita.application.port.OnboardingPort;
import io.audita.application.port.TenantSettingsPort;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.OAuthProvider;
import io.audita.domain.model.TenantStatus;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.AesEncryptionService;
import io.audita.infrastructure.tenant.FlywayTenantMigrator;
import io.audita.domain.model.ApprovalType;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Handles tenant lifecycle: provisioning (atomic), CRUD, domain whitelist, SSO
 * config.
 *
 * Provisioning is the critical path: schema creation + Flyway + Admin user +
 * invite
 * must all succeed or none of it persists (Outbox pattern simplified via single
 * TX).
 */
@Service
@Transactional
public class TenantService implements OnboardingPort, TenantSettingsPort {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String WORKFLOW_APPROVAL_TYPE_KEY = "workflow.approval_type_default";
    private static final String WORKFLOW_REQUIRE_DEFAULT_APPROVERS_KEY = "workflow.require_default_approvers";
    private static final String SLA_LOW_HOURS_KEY = "sla.deadline_hours.low";
    private static final String SLA_MEDIUM_HOURS_KEY = "sla.deadline_hours.medium";
    private static final String SLA_HIGH_HOURS_KEY = "sla.deadline_hours.high";
    private static final String SLA_CRITICAL_HOURS_KEY = "sla.deadline_hours.critical";
    private static final String SLA_WARNING_BEFORE_HOURS_KEY = "sla.warning_before_hours";
    private static final String WORKFLOW_DEFAULT_APPROVER_USER_IDS_KEY = "workflow.default_approver_user_ids";
    private static final String WORKFLOW_DEFAULT_APPROVER_GROUP_IDS_KEY = "workflow.default_approver_group_ids";
    private static final String ORG_PRIMARY_CONTACT_EMAIL_KEY = "org.primary_contact_email";
    private static final String ORG_TIMEZONE_KEY = "org.timezone";

    private final TenantRepository tenantRepository;
    private final OrgSettingRepository orgSettingRepository;
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
            OrgSettingRepository orgSettingRepository,
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
        this.orgSettingRepository = orgSettingRepository;
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

    @Cacheable(value = "onboardingStatus", key = "'slug'")
    @Transactional(readOnly = true)
    @Override
    public String findFirstTenantSlug() {
        return tenantRepository.findFirstByOrderByCreatedAtAsc()
                .map(TenantEntity::getSlug)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<TenantEntity> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public TenantEntity getTenant(UUID id) {
        return loadTenantOrThrow(id);
    }

    @Transactional(readOnly = true)
    public TenantEntity getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Tenant not found."));
    }

    @Transactional(readOnly = true)
    @Override
    public TenantSettings getTenantSettings(String tenantSlug) {
        TenantEntity tenant = getTenantBySlug(tenantSlug);
        TenantProfile profile = new TenantProfile(
                tenant.getName(),
                tenant.getSlug(),
                readStringSetting(ORG_PRIMARY_CONTACT_EMAIL_KEY, null),
                readStringSetting(ORG_TIMEZONE_KEY, "UTC"),
                tenant.getStatus().name());
        WorkflowDefaults workflowDefaults = new WorkflowDefaults(
                readApprovalTypeSetting(WORKFLOW_APPROVAL_TYPE_KEY, ApprovalType.LINEAR),
                readBooleanSetting(WORKFLOW_REQUIRE_DEFAULT_APPROVERS_KEY, true));
        SlaDefaults slaDefaults = new SlaDefaults(
                readIntSetting(SLA_LOW_HOURS_KEY, 72),
                readIntSetting(SLA_MEDIUM_HOURS_KEY, 48),
                readIntSetting(SLA_HIGH_HOURS_KEY, 24),
                readIntSetting(SLA_CRITICAL_HOURS_KEY, 8),
                readIntSetting(SLA_WARNING_BEFORE_HOURS_KEY, 1));
        AutoApproverDefaults autoApproverDefaults = new AutoApproverDefaults(
                readUuidListSetting(WORKFLOW_DEFAULT_APPROVER_USER_IDS_KEY),
                readUuidListSetting(WORKFLOW_DEFAULT_APPROVER_GROUP_IDS_KEY));
        return new TenantSettings(profile, workflowDefaults, slaDefaults, autoApproverDefaults);
    }

    @Override
    public void updateProfile(String tenantSlug, ProfileUpdate profile) {
        TenantEntity tenant = getTenantBySlug(tenantSlug);
        tenant.setName(profile.name());
        tenantRepository.save(tenant);
        saveSetting(ORG_PRIMARY_CONTACT_EMAIL_KEY,
                profile.primaryContactEmail() == null ? "" : profile.primaryContactEmail().trim());
        saveSetting(ORG_TIMEZONE_KEY, profile.timezone());
    }

    @Override
    public void updateWorkflowDefaults(String tenantSlug, WorkflowDefaults workflowDefaults) {
        getTenantBySlug(tenantSlug);
        saveSetting(WORKFLOW_APPROVAL_TYPE_KEY, workflowDefaults.approvalTypeDefault().name());
        saveSetting(WORKFLOW_REQUIRE_DEFAULT_APPROVERS_KEY,
                Boolean.toString(workflowDefaults.requireDefaultApprovers()));
    }

    @Override
    public void updateSlaDefaults(String tenantSlug, SlaDefaults slaDefaults) {
        getTenantBySlug(tenantSlug);
        saveSetting(SLA_LOW_HOURS_KEY, Integer.toString(slaDefaults.lowHours()));
        saveSetting(SLA_MEDIUM_HOURS_KEY, Integer.toString(slaDefaults.mediumHours()));
        saveSetting(SLA_HIGH_HOURS_KEY, Integer.toString(slaDefaults.highHours()));
        saveSetting(SLA_CRITICAL_HOURS_KEY, Integer.toString(slaDefaults.criticalHours()));
        saveSetting(SLA_WARNING_BEFORE_HOURS_KEY, Integer.toString(slaDefaults.warningBeforeHours()));
    }

    @Override
    public void updateAutoApproverDefaults(String tenantSlug, AutoApproverDefaults autoApproverDefaults) {
        getTenantBySlug(tenantSlug);
        saveSetting(WORKFLOW_DEFAULT_APPROVER_USER_IDS_KEY, writeUuidListSetting(autoApproverDefaults.userIds()));
        saveSetting(WORKFLOW_DEFAULT_APPROVER_GROUP_IDS_KEY, writeUuidListSetting(autoApproverDefaults.groupIds()));
    }

    private TenantEntity loadTenantOrThrow(UUID id) {
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

        // 2. Run per-tenant Flyway migrations (creates schema + seeds
        // roles/permissions)
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
                adminUser.setRoles(new LinkedHashSet<>(List.of(adminRole)));
                adminUser.setStatus(UserStatus.PENDING);
                userRepository.save(adminUser);

                // 4. Issue invite token (48-hour expiry)
                String rawToken = generateSecureToken();
                String tokenHash = AuthService.sha256(rawToken);
                InviteTokenEntity invite = new InviteTokenEntity(
                        adminUser, tokenHash, OffsetDateTime.now().plusHours(48));
                inviteTokenRepository.save(invite);

                // 5. Send invite email asynchronously — failure logged but never rethrows
                emailService.sendInviteEmail(adminEmail, adminFullName, rawToken, name, normalisedSlug);

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
     * Creates the tenant and the Admin user with a password set directly (no
     * invite).
     * Guard: fails if any tenant already exists.
     */
    @Override
    @CacheEvict(value = "onboardingStatus", allEntries = true)
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
                adminUser.setRoles(new LinkedHashSet<>(List.of(adminRole)));
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
        TenantEntity tenant = loadTenantOrThrow(id);
        if (name != null && !name.isBlank()) {
            tenant.setName(name);
        }
        if (status != null) {
            tenant.setStatus(status);
        }
        return tenantRepository.save(tenant);
    }

    public void deleteTenant(UUID id) {
        TenantEntity tenant = loadTenantOrThrow(id);
        tenantRepository.delete(tenant);
        log.info("Tenant deleted: id={} slug={}", id, tenant.getSlug());
    }

    // ── Domain Whitelist ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TenantAllowedDomainEntity> listDomains(UUID tenantId) {
        TenantEntity tenant = loadTenantOrThrow(tenantId);
        return allowedDomainRepository.findByTenantSlug(tenant.getSlug());
    }

    public TenantAllowedDomainEntity addDomain(UUID tenantId, String domain) {
        TenantEntity tenant = loadTenantOrThrow(tenantId);
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
        TenantEntity tenant = loadTenantOrThrow(tenantId);
        return ssoConfigRepository.findByTenantId(tenant.getId());
    }

    public TenantSsoConfigEntity upsertSsoConfig(UUID tenantId, OAuthProvider provider,
            String clientId, String rawClientSecret,
            String msTenantId) {
        TenantEntity tenant = loadTenantOrThrow(tenantId);
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

    private void saveSetting(String key, String value) {
        OrgSettingEntity existing = orgSettingRepository.findById(key).orElse(null);
        if (existing == null) {
            orgSettingRepository.save(new OrgSettingEntity(key, value));
        } else {
            existing.setValue(value);
            orgSettingRepository.save(existing);
        }
    }

    private String readSetting(String key) {
        return orgSettingRepository.findById(key)
                .map(OrgSettingEntity::getValue)
                .orElse(null);
    }

    private int readIntSetting(String key, int defaultValue) {
        String value = readSetting(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean readBooleanSetting(String key, boolean defaultValue) {
        String value = readSetting(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private ApprovalType readApprovalTypeSetting(String key, ApprovalType defaultValue) {
        String value = readSetting(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return ApprovalType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    private List<UUID> readUuidListSetting(String key) {
        return orgSettingRepository.findById(key)
                .map(OrgSettingEntity::getValue)
                .stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(UUID::fromString)
                .distinct()
                .toList();
    }

    private String readStringSetting(String key, String fallback) {
        return orgSettingRepository.findById(key)
                .map(OrgSettingEntity::getValue)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(fallback);
    }

    private String writeUuidListSetting(List<UUID> values) {
        return values.stream()
                .distinct()
                .map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(","));
    }
}
