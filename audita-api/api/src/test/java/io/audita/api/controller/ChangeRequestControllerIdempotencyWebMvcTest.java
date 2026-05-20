package io.audita.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.audita.api.security.UserPrincipal;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.service.ChangeRequestService;
import io.audita.infrastructure.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChangeRequestControllerIdempotencyWebMvcTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ChangeRequestService changeRequestService;

    @Mock
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        ChangeRequestController controller = new ChangeRequestController(changeRequestService, idempotencyService);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void create_with_duplicate_idempotency_key_replays_existing_change_request() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID changeRequestId = UUID.randomUUID();
        String key = "req-12345678";

        UserPrincipal principal = UserPrincipal.ofTenantUser(
                userId,
                "requester@acme.io",
                "REQUESTER",
                List.of("REQUESTER"),
                List.of(),
                "tenant-acme");

        ChangeRequestEntity existing = changeRequest(changeRequestId, userId, "Replay me");

        when(idempotencyService.findResourceId(eq(userId), eq("cr:create"), eq(key)))
                .thenReturn(Optional.of(changeRequestId));
        when(changeRequestService.getById(changeRequestId, userId)).thenReturn(existing);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            mockMvc.perform(post("/api/v1/change-requests")
                            .header("X-Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createPayload("Replay me"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(changeRequestId.toString()))
                    .andExpect(jsonPath("$.title").value("Replay me"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(changeRequestService, never()).create(any());
        verify(idempotencyService, never()).recordResource(any(), any(), any(), any());
    }

    @Test
    void create_with_new_idempotency_key_persists_dedupe_record() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID changeRequestId = UUID.randomUUID();
        String key = "req-abcdefgh";

        UserPrincipal principal = UserPrincipal.ofTenantUser(
                userId,
                "requester@acme.io",
                "REQUESTER",
                List.of("REQUESTER"),
                List.of(),
                "tenant-acme");

        ChangeRequestEntity created = changeRequest(changeRequestId, userId, "New CR");

        when(idempotencyService.findResourceId(eq(userId), eq("cr:create"), eq(key)))
                .thenReturn(Optional.empty());
        when(changeRequestService.create(any())).thenReturn(created);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            mockMvc.perform(post("/api/v1/change-requests")
                            .header("X-Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createPayload("New CR"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(changeRequestId.toString()))
                    .andExpect(jsonPath("$.title").value("New CR"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(idempotencyService).recordResource(userId, "cr:create", key, changeRequestId);
    }

    private ChangeRequestEntity changeRequest(UUID id, UUID userId, String title) {
        UserEntity user = new UserEntity("requester@acme.io", "Requester One");
        ReflectionTestUtils.setField(user, "id", userId);

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        ReflectionTestUtils.setField(changeRequest, "id", id);
        changeRequest.setTitle(title);
        changeRequest.setDescription("desc");
        changeRequest.setPriority(Priority.HIGH);
        changeRequest.setRiskLevel(RiskLevel.MEDIUM);
        changeRequest.setCategory("Infrastructure");
        changeRequest.setApprovalType(ApprovalType.NON_LINEAR);
        changeRequest.setStatus(ChangeRequestStatus.DRAFT);
        changeRequest.setCreatedBy(user);
        return changeRequest;
    }

    private Object createPayload(String title) {
        return java.util.Map.of(
                "title", title,
                "description", "desc",
                "priority", "HIGH",
                "riskLevel", "MEDIUM",
                "category", "Infrastructure",
                "approvalType", "NON_LINEAR",
                "affectedSystems", List.of("api"));
    }
}
