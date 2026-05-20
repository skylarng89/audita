package io.audita.infrastructure.tenant;

import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On application startup, iterates all existing tenants and applies any pending
 * per-tenant Flyway migrations. This ensures schemas created before a new
 * migration was introduced (e.g. {@code V6__add_user_roles.sql}) receive the
 * same schema updates as newly provisioned tenants.
 * <p>
 * Implemented as {@link SmartLifecycle} with a high negative phase so migrations
 * run <em>before</em> scheduled jobs (e.g. {@code SlaMonitoringService}) start.
 */
@Component
public class TenantMigrationStartupRunner implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationStartupRunner.class);

    /**
     * Phase set to a high negative value so this lifecycle bean starts before
     * most other components. Spring's default {@code @Scheduled} infrastructure
     * starts at phase 0, so a negative phase guarantees migrations complete
     * before any scheduled tasks execute.
     */
    private static final int MIGRATION_PHASE = Integer.MIN_VALUE + 1000;

    private final TenantRepository tenantRepository;
    private final FlywayTenantMigrator flywayTenantMigrator;

    private volatile boolean running = false;

    public TenantMigrationStartupRunner(TenantRepository tenantRepository,
                                        FlywayTenantMigrator flywayTenantMigrator) {
        this.tenantRepository = tenantRepository;
        this.flywayTenantMigrator = flywayTenantMigrator;
    }

    @Override
    public void start() {
        List<TenantEntity> tenants = tenantRepository.findAll();
        if (tenants.isEmpty()) {
            log.info("No existing tenants found; skipping startup migration.");
            running = true;
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
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return MIGRATION_PHASE;
    }
}
