package io.audita.api.controller;

import io.audita.api.security.UserPrincipal;
import io.audita.application.port.TenantSettingsPort;
import io.audita.domain.model.ApprovalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenantSettingsControllerWebMvcTest {

    MockMvc mockMvc;

    @Mock
    TenantSettingsPort tenantSettingsPort;

    @BeforeEach
    void setUp() {
        TenantSettingsController controller = new TenantSettingsController(tenantSettingsPort);
        ReflectionTestUtils.setField(controller, "jwtExpirySeconds", 900);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void get_settings_returns_workflow_and_sla_defaults() throws Exception {
        UserPrincipal principal = authenticate();
        when(tenantSettingsPort.getTenantSettings("acme")).thenReturn(
                new TenantSettingsPort.TenantSettings(
                        new TenantSettingsPort.TenantProfile("Acme Corp", "acme", "ACTIVE"),
                        new TenantSettingsPort.WorkflowDefaults(ApprovalType.LINEAR, true),
                        new TenantSettingsPort.SlaDefaults(72, 48, 24, 8, 1)
                )
        );

        try {
            mockMvc.perform(get("/api/v1/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profile.slug").value("acme"))
                    .andExpect(jsonPath("$.workflowDefaults.approvalTypeDefault").value("LINEAR"))
                    .andExpect(jsonPath("$.slaDefaults.criticalHours").value(8));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(tenantSettingsPort).getTenantSettings(eq(principal.tenantSlug()));
    }

    @Test
    void patch_settings_updates_workflow_and_sla_defaults() throws Exception {
        authenticate();
        when(tenantSettingsPort.getTenantSettings("acme")).thenReturn(
                new TenantSettingsPort.TenantSettings(
                        new TenantSettingsPort.TenantProfile("Acme Corp", "acme", "ACTIVE"),
                        new TenantSettingsPort.WorkflowDefaults(ApprovalType.NON_LINEAR, false),
                        new TenantSettingsPort.SlaDefaults(96, 72, 36, 12, 3)
                )
        );

        String body = """
                {
                  "workflowDefaults": {
                    "approvalTypeDefault": "NON_LINEAR",
                    "requireDefaultApprovers": false
                  },
                  "slaDefaults": {
                    "lowHours": 96,
                    "mediumHours": 72,
                    "highHours": 36,
                    "criticalHours": 12,
                    "warningBeforeHours": 3
                  }
                }
                """;

        try {
            mockMvc.perform(patch("/api/v1/settings")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workflowDefaults.approvalTypeDefault").value("NON_LINEAR"))
                    .andExpect(jsonPath("$.slaDefaults.warningBeforeHours").value(3));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(tenantSettingsPort).updateWorkflowDefaults(eq("acme"), any(TenantSettingsPort.WorkflowDefaults.class));
        verify(tenantSettingsPort).updateSlaDefaults(eq("acme"), any(TenantSettingsPort.SlaDefaults.class));
    }

    @Test
    void patch_settings_rejects_warning_hours_greater_than_deadline() throws Exception {
        authenticate();

        String body = """
                {
                  "workflowDefaults": {
                    "approvalTypeDefault": "LINEAR",
                    "requireDefaultApprovers": true
                  },
                  "slaDefaults": {
                    "lowHours": 72,
                    "mediumHours": 48,
                    "highHours": 24,
                    "criticalHours": 8,
                    "warningBeforeHours": 8
                  }
                }
                """;

        try {
            mockMvc.perform(patch("/api/v1/settings")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private UserPrincipal authenticate() {
        UserPrincipal principal = UserPrincipal.ofTenantUser(
                UUID.randomUUID(),
                "admin@acme.com",
                "ADMIN",
                "acme"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        return principal;
    }
}