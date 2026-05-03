package io.audita.api.controller;

import io.audita.api.dto.request.BootstrapRequest;
import io.audita.api.dto.request.SetupRequest;
import io.audita.application.port.AuthPort;
import io.audita.application.port.OnboardingPort;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/v1")
public class PlatformBootstrapController {

    private static final Logger log = LoggerFactory.getLogger(PlatformBootstrapController.class);

    private final AuthPort authService;
    private final OnboardingPort onboardingPort;

    @Value("${audita.bootstrap.setup-token:}")
    private String setupToken;

    public PlatformBootstrapController(AuthPort authService,
                                       OnboardingPort onboardingPort) {
        this.authService = authService;
        this.onboardingPort = onboardingPort;
    }

    /**
     * One-time platform bootstrap. Creates the first Super Admin account.
     * Publicly accessible — fails idempotently if already bootstrapped.
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<Map<String, String>> bootstrap(
            @Valid @RequestBody BootstrapRequest request,
            @RequestHeader(value = "X-Setup-Token", required = false) String providedSetupToken) {
        log.info("Bootstrap attempt: emailDomain={} setupTokenProvided={} onboardingCompleted={}",
                extractDomain(request.email()),
                providedSetupToken != null && !providedSetupToken.isBlank(),
                authService.isOnboardingCompleted());

        if (setupToken != null && !setupToken.isBlank() && !setupToken.equals(providedSetupToken)) {
            log.warn("Bootstrap rejected due to invalid setup token: emailDomain={}",
                    extractDomain(request.email()));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid setup token.");
        }
        authService.bootstrap(request.fullName(), request.email(), request.password());
        log.info("Bootstrap succeeded: emailDomain={}", extractDomain(request.email()));
        return ResponseEntity.ok(Map.of("message",
                "Platform bootstrapped. You can now sign in as Super Admin."));
    }

    /**
     * Single-tenant first-run setup. Creates the organisation and the first Admin account
     * in one atomic operation. Replaces the multi-step bootstrap + tenant provisioning flow.
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(@Valid @RequestBody SetupRequest request) {
        log.info("Single-tenant setup attempt: orgName={} slug={} emailDomain={}",
                request.orgName(), request.slug(), extractDomain(request.email()));

        onboardingPort.setupSingleTenant(
                request.orgName(),
                request.slug(),
                request.fullName(),
                request.email(),
            request.password());

        log.info("Single-tenant setup succeeded: slug={}", request.slug());
        return ResponseEntity.ok(Map.of(
                "message", "Organisation set up successfully.",
                "tenantSlug", request.slug()));
    }

    @GetMapping("/bootstrap/status")
    public ResponseEntity<Map<String, Object>> bootstrapStatus() {
        boolean onboardingCompleted = authService.isOnboardingCompleted();
        log.info("Bootstrap status requested: onboardingCompleted={}", onboardingCompleted);

        Map<String, Object> body = new HashMap<>();
        body.put("onboardingCompleted", onboardingCompleted);

        // Include the tenant slug so the frontend can use it for single-tenant login
        // without requiring a subdomain or query param.
        String slug = onboardingPort.findFirstTenantSlug();
        if (slug != null) {
            body.put("tenantSlug", slug);
        }

        return ResponseEntity.ok(body);
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }
}
