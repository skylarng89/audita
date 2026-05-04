package io.audita.api.controller;

import io.audita.api.dto.response.TenantAdminSettingsResponse;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.service.TenantService;
import io.audita.infrastructure.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class TenantSettingsController {

    private final TenantService tenantService;

    @Value("${audita.jwt.expiry-seconds:900}")
    private int jwtExpirySeconds;

    public TenantSettingsController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public TenantAdminSettingsResponse getSettings() {
        String tenantSlug = TenantContext.getCurrentTenant();
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new DomainNotPermittedException("TENANT_CONTEXT_REQUIRED", "Tenant context is required.");
        }

        TenantEntity tenant = tenantService.getTenantBySlug(tenantSlug);

        TenantAdminSettingsResponse.OrganizationProfile profile =
                new TenantAdminSettingsResponse.OrganizationProfile(
                        tenant.getName(),
                        tenant.getSlug(),
                        null,
                        "UTC",
                        tenant.getStatus().name()
                );

        TenantAdminSettingsResponse.FeatureFlags featureFlags =
                new TenantAdminSettingsResponse.FeatureFlags(false, false, false);

        TenantAdminSettingsResponse.SecurityDefaults securityDefaults =
                new TenantAdminSettingsResponse.SecurityDefaults(
                        Math.max(1, jwtExpirySeconds / 60),
                        "Not configured",
                        "Minimum 12 chars with upper, lower, number, and symbol"
                );

        return new TenantAdminSettingsResponse(profile, featureFlags, securityDefaults);
    }
}