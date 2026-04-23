package io.audita.infrastructure.tenant;

/**
 * Thread-local holder for the current tenant's schema name.
 * Set by TenantResolutionFilter on every incoming request.
 * Cleared after the request completes to prevent thread-pool leaks.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(String tenantSlug) {
        CURRENT_TENANT.set(tenantSlug);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
