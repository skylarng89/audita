package io.audita.api.controller;

import io.audita.api.dto.response.TenantAdminSettingsResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.application.port.TenantSettingsPort;
import io.audita.domain.exception.DomainNotPermittedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class TenantSettingsController {

        private final TenantSettingsPort tenantSettingsPort;

    @Value("${audita.jwt.expiry-seconds:900}")
    private int jwtExpirySeconds;

        public TenantSettingsController(TenantSettingsPort tenantSettingsPort) {
                this.tenantSettingsPort = tenantSettingsPort;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public TenantAdminSettingsResponse getSettings(@AuthenticationPrincipal UserPrincipal principal) {
                String tenantSlug = principal == null ? null : principal.tenantSlug();
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new DomainNotPermittedException("TENANT_CONTEXT_REQUIRED", "Tenant context is required.");
        }

                TenantSettingsPort.TenantProfile tenant = tenantSettingsPort.getTenantProfile(tenantSlug);

        TenantAdminSettingsResponse.OrganizationProfile profile =
                new TenantAdminSettingsResponse.OrganizationProfile(
                        tenant.name(),
                        tenant.slug(),
                        null,
                        "UTC",
                        tenant.status()
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