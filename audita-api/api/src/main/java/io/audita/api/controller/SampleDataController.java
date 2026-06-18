package io.audita.api.controller;

import io.audita.api.dto.response.SampleDataResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.application.port.SampleDataPort;
import io.audita.application.port.SampleDataPort.SampleDataSummary;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/sample-data")
public class SampleDataController {

    private final SampleDataPort sampleDataPort;

    public SampleDataController(SampleDataPort sampleDataPort) {
        this.sampleDataPort = sampleDataPort;
    }

    @PostMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'settings.manage')")
    public SampleDataResponse importSampleData(@AuthenticationPrincipal UserPrincipal principal) {
        String tenantSlug = resolveTenantSlug(principal);
        SampleDataSummary summary = sampleDataPort.importSampleData(tenantSlug);
        return toResponse(summary);
    }

    @DeleteMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'settings.manage')")
    public SampleDataResponse removeSampleData(@AuthenticationPrincipal UserPrincipal principal) {
        String tenantSlug = resolveTenantSlug(principal);
        SampleDataSummary summary = sampleDataPort.removeSampleData(tenantSlug);
        return toResponse(summary);
    }

    private String resolveTenantSlug(UserPrincipal principal) {
        String tenantSlug = principal == null ? null : principal.tenantSlug();
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context is required.");
        }
        return tenantSlug;
    }

    private SampleDataResponse toResponse(SampleDataSummary summary) {
        return new SampleDataResponse(
                summary.usersCount(),
                summary.groupsCount(),
                summary.changeRequestsCount(),
                summary.commentsCount(),
                summary.customFieldsCount(),
                summary.message());
    }
}