package io.audita.infrastructure.tenant;

import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * On application startup, iterates all existing tenants and applies any pending
 * per-tenant Flyway migrations. This ensures schemas created before a new
 * migration was introduced (e.g. {@code V6__add_user_roles.sql}) receive the
 * same schema updates as newly provisioned tenants.
 * <p>
 * Implemented as {@link SmartLifecycle} with a high negative phase so migrations
 * run <em>before</em> scheduled jobs (e.g. {@code SlaMonitoringService}) start.
 * <p>
 * Uses a PostgreSQL advisory lock to prevent multiple container replicas from
 * running migrations concurrently. Replicas that fail to acquire the lock skip
 * migration and proceed with startup.
 */
@Component
public class TenantMigrationStartupRunner implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationStartupRunner.class);

    /**
     * Fixed 64-bit advisory lock ID for tenant migrations. Must not clash with
     * any other advisory lock used in the application.
     */
    private static final long TENANT_MIGRATION_LOCK_ID = 42_001L;

    /**
     * Phase set to a high negative value so this lifecycle bean starts before
     * most other components. Spring's default {@code @Scheduled} infrastructure
     * starts at phase 0, so a negative phase guarantees migrations complete
     * before any scheduled tasks execute.
     */
    private static final int MIGRATION_PHASE = Integer.MIN_VALUE + 1000;

    private final TenantRepository tenantRepository;
    private final FlywayTenantMigrator flywayTenantMigrator;
    private final DataSource dataSource;
    private final boolean enabled;

    private volatile boolean running = false;

    public TenantMigrationStartupRunner(TenantRepository tenantRepository,
                                        FlywayTenantMigrator flywayTenantMigrator,
                                        DataSource dataSource,
                                        @Value("${audita.migration.startup.enabled:true}") boolean enabled) {
        this.tenantRepository = tenantRepository;
        this.flywayTenantMigrator = flywayTenantMigrator;
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @Override
    public void start() {
        if (!enabled) {
            log.info("Startup tenant migrations are disabled (audita.migration.startup.enabled=false).");
            running = true;
            return;
        }

        if (!acquireLock()) {
            log.info("Another instance is currently running tenant migrations; skipping startup migration.");
            running = true;
            return;
        }

        try {
            runMigrations();
        } finally {
            releaseLock();
        }
        running = true;
    }

    private boolean acquireLock() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT pg_try_advisory_lock(" + TENANT_MIGRATION_LOCK_ID + ")")) {
            if (rs.next()) {
                boolean acquired = rs.getBoolean(1);
                if (acquired) {
                    log.debug("Acquired advisory lock {} for tenant migrations.", TENANT_MIGRATION_LOCK_ID);
                }
                return acquired;
            }
        } catch (SQLException e) {
            log.warn("Failed to acquire advisory lock for tenant migrations; proceeding without lock.", e);
        }
        return false;
    }

    private void releaseLock() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeQuery("SELECT pg_advisory_unlock(" + TENANT_MIGRATION_LOCK_ID + ")");
            log.debug("Released advisory lock {} for tenant migrations.", TENANT_MIGRATION_LOCK_ID);
        } catch (SQLException e) {
            log.warn("Failed to release advisory lock {} for tenant migrations.", TENANT_MIGRATION_LOCK_ID, e);
        }
    }

    private void runMigrations() {
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
