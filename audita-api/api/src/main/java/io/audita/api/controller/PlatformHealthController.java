package io.audita.api.controller;

import io.audita.api.dto.response.PlatformHealthResponse;
import io.audita.application.port.OnboardingPort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/v1")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformHealthController {

    private final OnboardingPort onboardingPort;

    public PlatformHealthController(OnboardingPort onboardingPort) {
        this.onboardingPort = onboardingPort;
    }

    @GetMapping("/health")
    public PlatformHealthResponse health() {
        try {
            String firstTenantSlug = onboardingPort.findFirstTenantSlug();
            String detail = "Database reachable; firstTenantSlug="
                    + (firstTenantSlug == null ? "none" : firstTenantSlug);
            return new PlatformHealthResponse("UP", 100, detail);
        } catch (Exception _) {
            return new PlatformHealthResponse("DOWN", 0, "Database check failed");
        }
    }
}