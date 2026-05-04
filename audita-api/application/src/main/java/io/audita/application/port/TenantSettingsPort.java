package io.audita.application.port;

public interface TenantSettingsPort {

    TenantProfile getTenantProfile(String tenantSlug);

    record TenantProfile(
            String name,
            String slug,
            String status
    ) {
    }
}