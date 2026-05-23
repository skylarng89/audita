package io.audita.api.controller;

import io.audita.api.dto.request.*;
import io.audita.api.dto.response.*;
import io.audita.infrastructure.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Platform-level tenant management — Super Admin only.
 * Handles tenant CRUD, provisioning, domain whitelist, and SSO config.
 */
@RestController
@RequestMapping("/api/platform/v1/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public PageResponse<TenantResponse> listTenants(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(tenantService.listTenants(pageable), TenantResponse::from);
    }

    @GetMapping("/{id}")
    public TenantResponse getTenant(@PathVariable UUID id) {
        return TenantResponse.from(tenantService.getTenant(id));
    }

    @PostMapping
    public ResponseEntity<TenantResponse> provisionTenant(@Valid @RequestBody ProvisionTenantRequest req) {
        var tenant = tenantService.provision(req.name(), req.slug(), req.adminEmail(), req.adminFullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    @PatchMapping("/{id}")
    public TenantResponse updateTenant(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateTenantRequest req) {
        return TenantResponse.from(tenantService.updateTenant(id, req.name(), req.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
    }

    // ── Domain Whitelist ───────────────────────────────────────────────────────

    @GetMapping("/{id}/domains")
    public List<DomainResponse> listDomains(@PathVariable UUID id) {
        return tenantService.listDomains(id).stream().map(DomainResponse::from).toList();
    }

    @PostMapping("/{id}/domains")
    public ResponseEntity<DomainResponse> addDomain(@PathVariable UUID id,
                                                     @Valid @RequestBody AddDomainRequest req) {
        var domain = tenantService.addDomain(id, req.domain());
        return ResponseEntity.status(HttpStatus.CREATED).body(DomainResponse.from(domain));
    }

    @DeleteMapping("/{id}/domains/{domainId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDomain(@PathVariable UUID id, @PathVariable UUID domainId) {
        tenantService.removeDomain(domainId);
    }

    // SSO intentionally removed (not implemented yet)
}
