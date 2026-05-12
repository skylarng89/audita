package io.audita.api.controller;

import io.audita.api.dto.response.PageResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.application.port.AuditTrailPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuditTrailControllerWebMvcTest {

    MockMvc mockMvc;

    @Mock
    AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        AuditTrailController controller = new AuditTrailController(auditTrailPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private void authenticate(String role) {
        UserPrincipal principal = UserPrincipal.ofTenantUser(
                UUID.randomUUID(), "admin@acme.com", role, "acme");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void query_returns_paged_audit_log() throws Exception {
        authenticate("ADMIN");

        UUID entryId = UUID.randomUUID();
        UUID crId = UUID.randomUUID();
        AuditTrailPort.AuditLogEntry entry = new AuditTrailPort.AuditLogEntry(
                entryId, UUID.randomUUID(), "admin@acme.com", "CR_SUBMITTED",
                "change_request", crId, null, "127.0.0.1",
                OffsetDateTime.now()
        );
        when(auditTrailPort.query(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(entry),
                        PageRequest.of(0, 20, Sort.by("createdAt")), 1));

        mockMvc.perform(get("/api/v1/audit-trail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].actionType").value("CR_SUBMITTED"))
                .andExpect(jsonPath("$.content[0].entityType").value("change_request"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void query_filters_by_action_type() throws Exception {
        authenticate("AUDITOR");

        when(auditTrailPort.query(isNull(), eq("CR_APPROVED"), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/audit-trail").param("actionType", "CR_APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void export_csv_returns_csv_content_type() throws Exception {
        authenticate("ADMIN");

        AuditTrailPort.AuditLogEntry entry = new AuditTrailPort.AuditLogEntry(
                UUID.randomUUID(), UUID.randomUUID(), "admin@acme.com", "CR_CREATED",
                "change_request", UUID.randomUUID(), null, null, OffsetDateTime.now()
        );
        when(auditTrailPort.export(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/audit-trail/export.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("audit-trail.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void export_csv_escapes_commas_in_fields() throws Exception {
        authenticate("ADMIN");

        AuditTrailPort.AuditLogEntry entry = new AuditTrailPort.AuditLogEntry(
                UUID.randomUUID(), UUID.randomUUID(), "admin,comma@acme.com", "CR_CREATED",
                "change_request", UUID.randomUUID(), null, null, OffsetDateTime.now()
        );
        when(auditTrailPort.export(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/audit-trail/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"admin,comma@acme.com\"")));
    }

    @Test
    void query_with_date_filters_delegates_to_port() throws Exception {
        authenticate("ADMIN");

        when(auditTrailPort.query(isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 3, 31)), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/audit-trail")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    void query_without_authentication_throws() {
        SecurityContextHolder.clearContext();
        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/v1/audit-trail")).andReturn());
    }
}
