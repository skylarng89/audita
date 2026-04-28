package io.audita.api.controller;

import io.audita.api.dto.request.BootstrapRequest;
import io.audita.infrastructure.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/v1")
public class PlatformBootstrapController {

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
        if (setupToken != null && !setupToken.isBlank() && !setupToken.equals(providedSetupToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid setup token.");
        }
        authService.bootstrap(request.fullName(), request.email(), request.password());
        return ResponseEntity.ok(Map.of("message",
                "Platform bootstrapped. You can now sign in as Super Admin."));
    }

    @GetMapping("/bootstrap/status")
    public ResponseEntity<Map<String, Boolean>> bootstrapStatus() {
        return ResponseEntity.ok(Map.of("onboardingCompleted", authService.isOnboardingCompleted()));
    }
}
