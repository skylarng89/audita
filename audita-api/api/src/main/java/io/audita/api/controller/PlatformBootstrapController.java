package io.audita.api.controller;

import io.audita.api.dto.request.BootstrapRequest;
import io.audita.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/v1")
@RequiredArgsConstructor
public class PlatformBootstrapController {

    private final AuthService authService;

    /**
     * One-time platform bootstrap. Creates the first Super Admin account.
     * Idempotent at the guard level — fails if already bootstrapped.
     * Publicly accessible (no auth) so the first admin can be created.
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<Map<String, String>> bootstrap(
            @Valid @RequestBody BootstrapRequest request) {
        authService.bootstrap(request.fullName(), request.email(), request.password());
        return ResponseEntity.ok(Map.of("message",
                "Platform bootstrapped. You can now sign in as Super Admin."));
    }
}
