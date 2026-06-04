package io.audita.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.service.RequestDeploymentService;
import io.audita.infrastructure.service.RequestUatService;
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

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RequestWorkflowControllersWebMvcTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RequestUatService requestUatService;

    @Mock
    private RequestDeploymentService requestDeploymentService;

    private final UUID requestId = UUID.randomUUID();
    private final UUID uatId = UUID.randomUUID();
    private final UUID deploymentId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        RequestUatController uatController = new RequestUatController(requestUatService);
        RequestDeploymentController deploymentController = new RequestDeploymentController(requestDeploymentService);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(uatController, deploymentController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        principal = UserPrincipal.ofTenantUser(
                userId,
                "requester@acme.io",
                "REQUESTER",
                List.of("REQUESTER"),
                List.of(),
                "tenant-acme");
    }

    @Test
    void create_uat_returns_201() throws Exception {
        RequestUatEntity created = uatEntity(uatId, requestId, "UAT Plan", "Details");

        when(requestUatService.createUat(eq(requestId), eq("UAT Plan"), eq("Details"),
                eq(userId), eq("REQUESTER"))).thenReturn(created);

        setAuth(principal);
        try {
            mockMvc.perform(post("/api/v1/change-requests/{requestId}/uat", requestId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("title", "UAT Plan", "details", "Details"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(uatId.toString()))
                    .andExpect(jsonPath("$.title").value("UAT Plan"))
                    .andExpect(jsonPath("$.requestId").value(requestId.toString()));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(requestUatService).createUat(requestId, "UAT Plan", "Details", userId, "REQUESTER");
    }

    @Test
    void update_uat_returns_200() throws Exception {
        RequestUatEntity existing = uatEntity(uatId, requestId, "UAT Plan", "Details");
        RequestUatEntity updated = uatEntity(uatId, requestId, "Updated Plan", "Updated Details");

        when(requestUatService.getByRequestId(requestId)).thenReturn(Optional.of(existing));
        when(requestUatService.updateUat(eq(uatId), eq("Updated Plan"), eq("Updated Details"),
                eq(userId), eq("REQUESTER"))).thenReturn(updated);

        setAuth(principal);
        try {
            mockMvc.perform(patch("/api/v1/change-requests/{requestId}/uat", requestId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("title", "Updated Plan", "details", "Updated Details"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Plan"))
                    .andExpect(jsonPath("$.details").value("Updated Details"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void promote_uat_returns_200() throws Exception {
        RequestUatEntity existing = uatEntity(uatId, requestId, "UAT Plan", "Details");
        RequestUatEntity promoted = uatEntity(uatId, requestId, "UAT Plan", "Details");
        promoted.setStatus("PROMOTED");
        promoted.setReadOnly(true);

        when(requestUatService.getByRequestId(requestId)).thenReturn(Optional.of(existing));
        when(requestUatService.promoteToDeployment(eq(uatId), eq(userId), eq("REQUESTER")))
                .thenReturn(promoted);

        setAuth(principal);
        try {
            mockMvc.perform(post("/api/v1/change-requests/{requestId}/uat/promote", requestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROMOTED"))
                    .andExpect(jsonPath("$.readOnly").value(true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void approve_deployment_returns_204() throws Exception {
        RequestDeploymentEntity deployment = deploymentEntity(deploymentId, requestId, uatId);

        when(requestDeploymentService.getByRequestId(requestId)).thenReturn(Optional.of(deployment));

        setAuth(principal);
        try {
            mockMvc.perform(post("/api/v1/change-requests/{requestId}/deployment/approve", requestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(requestDeploymentService).approveDeployment(deploymentId, userId);
    }

    @Test
    void reject_deployment_returns_204() throws Exception {
        RequestDeploymentEntity deployment = deploymentEntity(deploymentId, requestId, uatId);

        when(requestDeploymentService.getByRequestId(requestId)).thenReturn(Optional.of(deployment));

        setAuth(principal);
        try {
            mockMvc.perform(post("/api/v1/change-requests/{requestId}/deployment/reject", requestId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("reason", "Not ready for production"))))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(requestDeploymentService).rejectDeployment(deploymentId, userId, "Not ready for production");
    }

    @Test
    void deployment_controller_has_no_create_endpoint() {
        Method[] methods = RequestDeploymentController.class.getDeclaredMethods();
        boolean hasCreateMethod = Arrays.stream(methods)
                .anyMatch(m -> m.getName().equals("create") || m.getName().equals("createDeployment"));
        assertThat(hasCreateMethod).isFalse();
    }

    @Test
    void get_uat_returns_200() throws Exception {
        RequestUatEntity uat = uatEntity(uatId, requestId, "UAT Plan", "Details");

        when(requestUatService.getByRequestId(requestId)).thenReturn(Optional.of(uat));

        mockMvc.perform(get("/api/v1/change-requests/{requestId}/uat", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uatId.toString()))
                .andExpect(jsonPath("$.title").value("UAT Plan"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.readOnly").value(false));
    }

    private RequestUatEntity uatEntity(UUID id, UUID requestId, String title, String details) {
        RequestUatEntity entity = new RequestUatEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setRequestId(requestId);
        entity.setTitle(title);
        entity.setDetails(details);
        entity.setCreatedBy(userId);
        ReflectionTestUtils.setField(entity, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(entity, "updatedAt", OffsetDateTime.now());
        return entity;
    }

    private RequestDeploymentEntity deploymentEntity(UUID id, UUID requestId, UUID uatId) {
        RequestDeploymentEntity entity = new RequestDeploymentEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setRequestId(requestId);
        entity.setUatId(uatId);
        entity.setCreatedBy(userId);
        entity.setPromotedAt(OffsetDateTime.now());
        return entity;
    }

    private void setAuth(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
