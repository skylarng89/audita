package io.audita.api.controller;

import io.audita.api.dto.request.PatchTenantAdminSettingsRequest;
import io.audita.api.dto.response.TenantAdminSettingsResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.application.port.TenantSettingsPort;
import io.audita.domain.exception.DomainNotPermittedException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

                TenantSettingsPort.TenantSettings settings = tenantSettingsPort.getTenantSettings(tenantSlug);
                TenantSettingsPort.TenantProfile tenant = settings.profile();

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

        TenantAdminSettingsResponse.WorkflowDefaults workflowDefaults =
                new TenantAdminSettingsResponse.WorkflowDefaults(
                        settings.workflowDefaults().approvalTypeDefault(),
                        settings.workflowDefaults().requireDefaultApprovers()
                );

        TenantAdminSettingsResponse.SlaDefaults slaDefaults =
                new TenantAdminSettingsResponse.SlaDefaults(
                        settings.slaDefaults().lowHours(),
                        settings.slaDefaults().mediumHours(),
                        settings.slaDefaults().highHours(),
                        settings.slaDefaults().criticalHours(),
                        settings.slaDefaults().warningBeforeHours()
                );

        return new TenantAdminSettingsResponse(profile, featureFlags, securityDefaults, workflowDefaults, slaDefaults);
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public TenantAdminSettingsResponse patchSettings(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody PatchTenantAdminSettingsRequest request) {
        String tenantSlug = principal == null ? null : principal.tenantSlug();
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new DomainNotPermittedException("TENANT_CONTEXT_REQUIRED", "Tenant context is required.");
        }

        int minDeadline = Math.min(
                Math.min(request.slaDefaults().lowHours(), request.slaDefaults().mediumHours()),
                Math.min(request.slaDefaults().highHours(), request.slaDefaults().criticalHours())
        );
        if (request.slaDefaults().warningBeforeHours() >= minDeadline) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "warningBeforeHours must be lower than all SLA deadline hour values."
            );
        }

        tenantSettingsPort.updateWorkflowDefaults(
                tenantSlug,
                new TenantSettingsPort.WorkflowDefaults(
                        request.workflowDefaults().approvalTypeDefault(),
                        request.workflowDefaults().requireDefaultApprovers()
                )
        );
        tenantSettingsPort.updateSlaDefaults(
                tenantSlug,
                new TenantSettingsPort.SlaDefaults(
                        request.slaDefaults().lowHours(),
                        request.slaDefaults().mediumHours(),
                        request.slaDefaults().highHours(),
                        request.slaDefaults().criticalHours(),
                        request.slaDefaults().warningBeforeHours()
                )
        );

        return getSettings(principal);
    }
}