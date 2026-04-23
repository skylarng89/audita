package io.audita.api.controller;

import io.audita.api.dto.request.BootstrapRequest;
import io.audita.infrastructure.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/v1")
public class PlatformBootstrapController {

    private final AuthService authService;

    public PlatformBootstrapController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * One-time platform bootstrap. Creates the first Super Admin account.
     * Publicly accessible — fails idempotently if already bootstrapped.
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<Map<String, String>> bootstrap(
            @Valid @RequestBody BootstrapRequest request) {
        authService.bootstrap(request.fullName(), request.email(), request.password());
        return ResponseEntity.ok(Map.of("message",
                "Platform bootstrapped. You can now sign in as Super Admin."));
    }
}
