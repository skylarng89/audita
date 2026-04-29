package io.audita.api.controller;

import io.audita.api.dto.request.BootstrapRequest;
import io.audita.infrastructure.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/v1")
public class PlatformBootstrapController {

    private static final Logger log = LoggerFactory.getLogger(PlatformBootstrapController.class);

    private final AuthService authService;

    @Value("${audita.bootstrap.setup-token:}")
    private String setupToken;

    public PlatformBootstrapController(AuthService authService) {
        this.authService = authService;
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

    @GetMapping("/bootstrap/status")
    public ResponseEntity<Map<String, Boolean>> bootstrapStatus() {
        boolean onboardingCompleted = authService.isOnboardingCompleted();
        log.info("Bootstrap status requested: onboardingCompleted={}", onboardingCompleted);
        return ResponseEntity.ok(Map.of("onboardingCompleted", onboardingCompleted));
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }
}
