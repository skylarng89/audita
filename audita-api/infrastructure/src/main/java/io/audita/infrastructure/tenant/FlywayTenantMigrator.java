package io.audita.infrastructure.tenant;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Applies per-tenant Flyway migrations.
 * Called on:
 *  1. Application startup — migrates all existing tenant schemas via {@link TenantMigrationStartupRunner}.
 *  2. Tenant provisioning — migrates the newly created schema.
 *
 * Migrations are loaded from db/migration/tenant/ on the classpath.
 */
@Component
public class FlywayTenantMigrator {

    private static final Logger log = LoggerFactory.getLogger(FlywayTenantMigrator.class);
    private static final String MIGRATION_PATH = "classpath:db/migration/tenant";

    private final DataSource dataSource;

    public FlywayTenantMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate(String tenantSlug) {
        log.info("Running Flyway migrations for tenant schema: {}", tenantSlug);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(tenantSlug)
                .locations(MIGRATION_PATH)
                .table("flyway_schema_history")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load();

        try {
            flyway.migrate();
        } catch (FlywayException e) {
            log.warn("Flyway migration failed for tenant {}, repairing and retrying", tenantSlug, e);
            flyway.repair();
            flyway.migrate();
        }

        log.info("Flyway migrations complete for tenant: {}", tenantSlug);
    }
}
