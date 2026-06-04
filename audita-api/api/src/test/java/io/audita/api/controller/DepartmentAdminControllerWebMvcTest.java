package io.audita.api.controller;

import io.audita.api.exception.GlobalExceptionHandler;
import io.audita.api.security.UserPrincipal;
import io.audita.domain.exception.InvalidRequestException;
import io.audita.infrastructure.persistence.entity.DepartmentEntity;
import io.audita.infrastructure.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DepartmentAdminControllerWebMvcTest {

    MockMvc mockMvc;

    @Mock
    DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        DepartmentAdminController controller = new DepartmentAdminController(departmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void admin_can_list_all_departments() throws Exception {
        authenticateAdmin();
        when(departmentService.listAll()).thenReturn(List.of(
                buildEntity("Engineering", "ENG", true, 1),
                buildEntity("Finance", "FIN", true, 2)
        ));

        try {
            mockMvc.perform(get("/api/v1/settings/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Engineering"))
                    .andExpect(jsonPath("$[1].code").value("FIN"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(departmentService).listAll();
    }

    @Test
    void admin_can_create_department() throws Exception {
        authenticateAdmin();
        UUID id = UUID.randomUUID();
        when(departmentService.create(anyString(), anyString(), anyBoolean(), anyInt()))
                .thenReturn(buildEntityWithId(id, "Engineering", "ENG", true, 1));

        String body = """
                {
                  "name": "Engineering",
                  "code": "ENG",
                  "isActive": true,
                  "displayOrder": 1
                }
                """;

        try {
            mockMvc.perform(post("/api/v1/settings/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.name").value("Engineering"))
                    .andExpect(jsonPath("$.code").value("ENG"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(departmentService).create(eq("Engineering"), eq("ENG"), eq(true), eq(1));
    }

    @Test
    void admin_can_update_department() throws Exception {
        authenticateAdmin();
        UUID id = UUID.randomUUID();
        when(departmentService.update(eq(id), anyString(), anyString(), anyBoolean(), anyInt()))
                .thenReturn(buildEntityWithId(id, "Engineering Updated", "ENG", true, 2));

        String body = """
                {
                  "name": "Engineering Updated",
                  "code": "ENG",
                  "isActive": true,
                  "displayOrder": 2
                }
                """;

        try {
            mockMvc.perform(patch("/api/v1/settings/departments/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Engineering Updated"))
                    .andExpect(jsonPath("$.displayOrder").value(2));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(departmentService).update(eq(id), eq("Engineering Updated"), eq("ENG"), eq(true), eq(2));
    }

    @Test
    void admin_can_deactivate_department() throws Exception {
        authenticateAdmin();
        UUID id = UUID.randomUUID();

        try {
            mockMvc.perform(delete("/api/v1/settings/departments/{id}", id))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(departmentService).deactivate(id);
    }

    @Test
    void create_endpoint_requires_admin_role() throws Exception {
        Method method = DepartmentAdminController.class.getMethod("create",
                io.audita.api.dto.request.UpsertDepartmentRequest.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
    }

    @Test
    void any_authenticated_user_can_list_active_departments() throws Exception {
        authenticateRequester();
        when(departmentService.listActive()).thenReturn(List.of(
                buildEntity("Engineering", "ENG", true, 1)
        ));

        try {
            mockMvc.perform(get("/api/v1/settings/departments/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Engineering"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(departmentService).listActive();
    }

    @Test
    void create_with_duplicate_name_returns_400() throws Exception {
        authenticateAdmin();
        when(departmentService.create(anyString(), anyString(), anyBoolean(), anyInt()))
                .thenThrow(new InvalidRequestException("DUPLICATE_NAME", "Department name already exists."));

        String body = """
                {
                  "name": "Engineering",
                  "code": "ENG",
                  "isActive": true,
                  "displayOrder": 1
                }
                """;

        try {
            mockMvc.perform(post("/api/v1/settings/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private DepartmentEntity buildEntity(String name, String code, boolean active, int order) {
        return buildEntityWithId(UUID.randomUUID(), name, code, active, order);
    }

    private DepartmentEntity buildEntityWithId(UUID id, String name, String code, boolean active, int order) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setName(name);
        entity.setCode(code);
        entity.setActive(active);
        entity.setDisplayOrder(order);
        try {
            java.lang.reflect.Field idField = DepartmentEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception ignored) {}
        return entity;
    }

    private void authenticateAdmin() {
        UserPrincipal principal = UserPrincipal.ofTenantUser(
                UUID.randomUUID(),
                "admin@acme.com",
                "ADMIN",
                List.of("ADMIN"),
                List.of(),
                "acme");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void authenticateRequester() {
        UserPrincipal principal = UserPrincipal.ofTenantUser(
                UUID.randomUUID(),
                "user@acme.com",
                "REQUESTER",
                List.of("REQUESTER"),
                List.of(),
                "acme");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
