package io.audita.api.controller;

import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.persistence.entity.NotificationEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerWebMvcTest {

        MockMvc mockMvc;

        @Mock NotificationService notificationService;
        @Mock JwtService jwtService;

        @BeforeEach
        void setUp() {
                NotificationController controller = new NotificationController(notificationService, jwtService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                                .build();
        }

    @Test
    void list_returns_notifications_and_unread_header() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        UserPrincipal principal = UserPrincipal.ofTenantUser(
                userId,
                "user@example.com",
                "APPROVER",
                "tenant-acme"
        );

        UserEntity recipient = new UserEntity("user@example.com", "User One");
        ReflectionTestUtils.setField(recipient, "id", userId);
        NotificationEntity notification = new NotificationEntity(
                recipient,
                "MENTION",
                "Mentioned",
                "You were mentioned",
                "/change-requests/1"
        );
        ReflectionTestUtils.setField(notification, "id", notificationId);

        when(notificationService.list(userId, 0, 20)).thenReturn(List.of(notification));
        when(notificationService.unreadCount(userId)).thenReturn(5L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        try {
            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Unread-Count", "5"))
                    .andExpect(jsonPath("$[0].id").value(notificationId.toString()))
                    .andExpect(jsonPath("$[0].type").value("MENTION"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void mark_read_returns_no_content() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.ofTenantUser(
                userId,
                "user@example.com",
                "APPROVER",
                "tenant-acme"
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        try {
            mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(notificationService).markRead(userId, notificationId);
    }

    @Test
    void read_all_returns_no_content() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.ofTenantUser(
                userId,
                "user@example.com",
                "APPROVER",
                "tenant-acme"
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        try {
            mockMvc.perform(post("/api/v1/notifications/read-all"))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(notificationService).markAllRead(eq(userId));
    }

        @Test
        void issue_stream_token_returns_token_for_authenticated_user() throws Exception {
                UUID userId = UUID.randomUUID();
                UserPrincipal principal = UserPrincipal.ofTenantUser(
                                userId,
                                "user@example.com",
                                "APPROVER",
                                "tenant-acme"
                );

                when(jwtService.issueStreamToken(userId, "tenant-acme")).thenReturn("stream-token-123");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
                );
                try {
                        mockMvc.perform(post("/api/v1/notifications/stream-token"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.streamToken").value("stream-token-123"));
                } finally {
                        SecurityContextHolder.clearContext();
                }
        }

        @Test
        void stream_accepts_valid_stream_token_and_subscribes() throws Exception {
                UUID userId = UUID.randomUUID();
                SseEmitter emitter = new SseEmitter();

                when(jwtService.isValidStreamToken("valid-stream")).thenReturn(true);
                when(jwtService.parse("valid-stream")).thenReturn(io.jsonwebtoken.Jwts.claims().subject(userId.toString()).build());
                when(notificationService.subscribe(userId)).thenReturn(emitter);

                mockMvc.perform(get("/api/v1/notifications/stream").param("streamToken", "valid-stream"))
                                .andExpect(status().isOk());

                verify(notificationService).subscribe(userId);
        }

        @Test
        void stream_rejects_invalid_stream_token_when_principal_missing() throws Exception {
                when(jwtService.isValidStreamToken("invalid-stream")).thenReturn(false);

                jakarta.servlet.ServletException ex = assertThrows(jakarta.servlet.ServletException.class,
                                () -> mockMvc.perform(get("/api/v1/notifications/stream").param("streamToken", "invalid-stream")));
                assertInstanceOf(AccessDeniedException.class, ex.getCause());
        }
}
