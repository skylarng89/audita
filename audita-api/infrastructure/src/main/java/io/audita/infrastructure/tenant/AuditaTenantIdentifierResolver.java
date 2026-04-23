package io.audita.infrastructure.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the active PostgreSQL schema name for each Hibernate session.
 * Reads the tenant slug from TenantContext (set by TenantResolutionFilter).
 *
 * The public schema is used when no tenant is in context (platform-level operations).
 */
@Component
public class AuditaTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private static final String PUBLIC_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        return (tenant != null) ? tenant : PUBLIC_SCHEMA;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
