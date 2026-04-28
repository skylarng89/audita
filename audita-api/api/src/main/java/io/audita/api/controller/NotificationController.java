package io.audita.api.controller;

import io.audita.api.dto.response.NotificationResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.service.NotificationService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    public NotificationController(NotificationService notificationService,
                                  JwtService jwtService) {
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<NotificationResponse> items = notificationService.list(principal.userId(), page, size).stream()
                .map(NotificationResponse::from)
                .toList();
        long unread = notificationService.unreadCount(principal.userId());

        return ResponseEntity.ok()
                .header("X-Unread-Count", String.valueOf(unread))
                .body(items);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable UUID id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal,
                             @RequestParam(required = false) String accessToken) {
        UUID userId = resolveUserId(principal, accessToken);
        return notificationService.subscribe(userId);
    }

    private UUID resolveUserId(UserPrincipal principal, String accessToken) {
        if (principal != null) {
            return principal.userId();
        }
        if (accessToken != null && !accessToken.isBlank() && jwtService.isValid(accessToken)) {
            Claims claims = jwtService.parse(accessToken);
            return UUID.fromString(claims.getSubject());
        }
        throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
    }
}
