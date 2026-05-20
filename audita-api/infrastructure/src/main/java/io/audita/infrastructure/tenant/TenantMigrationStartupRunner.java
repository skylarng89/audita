package io.audita.infrastructure.tenant;

import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On application startup, iterates all existing tenants and applies any pending
 * per-tenant Flyway migrations. This ensures schemas created before a new
 * migration was introduced (e.g. {@code V6__add_user_roles.sql}) receive the
 * same schema updates as newly provisioned tenants.
 */
@Component
public class TenantMigrationStartupRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationStartupRunner.class);

    private final TenantRepository tenantRepository;
    private final FlywayTenantMigrator flywayTenantMigrator;

    public TenantMigrationStartupRunner(TenantRepository tenantRepository,
                                        FlywayTenantMigrator flywayTenantMigrator) {
        this.tenantRepository = tenantRepository;
        this.flywayTenantMigrator = flywayTenantMigrator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateAllTenantsOnStartup() {
        List<TenantEntity> tenants = tenantRepository.findAll();
        if (tenants.isEmpty()) {
            log.info("No existing tenants found; skipping startup migration.");
            return;
        }

        log.info("Running pending Flyway migrations for {} existing tenant(s)...", tenants.size());
        for (TenantEntity tenant : tenants) {
            String slug = tenant.getSlug();
            try {
                flywayTenantMigrator.migrate(slug);
            } catch (Exception e) {
                log.error("Failed to migrate tenant schema on startup: slug={}", slug, e);
                // Continue with other tenants — do not fail the entire startup.
            }
        }
        log.info("Startup tenant migrations complete.");
    }
}
