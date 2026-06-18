package io.audita.infrastructure.service;

import io.audita.application.port.TenantSettingsPort;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.TenantStatus;
import io.audita.infrastructure.persistence.entity.OrgSettingEntity;
import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.repository.InviteTokenRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.RoleRepository;
import io.audita.infrastructure.persistence.repository.TenantAllowedDomainRepository;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import io.audita.infrastructure.persistence.repository.TenantSsoConfigRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.security.AesEncryptionService;
import io.audita.infrastructure.tenant.FlywayTenantMigrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceSettingsTest {

    @Mock TenantRepository tenantRepository;
    @Mock OrgSettingRepository orgSettingRepository;
    @Mock TenantAllowedDomainRepository allowedDomainRepository;
    @Mock TenantSsoConfigRepository ssoConfigRepository;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock InviteTokenRepository inviteTokenRepository;
    @Mock FlywayTenantMigrator flywayTenantMigrator;
    @Mock EmailService emailService;
    @Mock AesEncryptionService aesEncryptionService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PlatformTransactionManager transactionManager;

    @InjectMocks
    TenantService tenantService;

    @Test
    void getTenantSettings_usesDefaultsWhenSettingsDoNotExist() {
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(activeTenant("acme")));
        when(orgSettingRepository.findById(any())).thenReturn(Optional.empty());

        TenantSettingsPort.TenantSettings settings = tenantService.getTenantSettings("acme");

        assertThat(settings.profile().name()).isEqualTo("Acme Corp");
        assertThat(settings.workflowDefaults().approvalTypeDefault()).isEqualTo(ApprovalType.LINEAR);
        assertThat(settings.workflowDefaults().requireDefaultApprovers()).isTrue();
        assertThat(settings.slaDefaults().lowHours()).isEqualTo(72);
        assertThat(settings.slaDefaults().mediumHours()).isEqualTo(48);
        assertThat(settings.slaDefaults().highHours()).isEqualTo(24);
        assertThat(settings.slaDefaults().criticalHours()).isEqualTo(8);
        assertThat(settings.slaDefaults().warningBeforeHours()).isEqualTo(1);
    }

    @Test
    void getTenantSettings_handlesMalformedValuesWithSafeFallbacks() {
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(activeTenant("acme")));
        when(orgSettingRepository.findById(any())).thenReturn(Optional.empty());
        when(orgSettingRepository.findById("workflow.approval_type_default"))
                .thenReturn(Optional.of(new OrgSettingEntity("workflow.approval_type_default", "invalid")));
        when(orgSettingRepository.findById("workflow.require_default_approvers"))
                .thenReturn(Optional.of(new OrgSettingEntity("workflow.require_default_approvers", "false")));
        when(orgSettingRepository.findById("sla.deadline_hours.low"))
                .thenReturn(Optional.of(new OrgSettingEntity("sla.deadline_hours.low", "-5")));
        when(orgSettingRepository.findById("sla.deadline_hours.medium"))
                .thenReturn(Optional.of(new OrgSettingEntity("sla.deadline_hours.medium", "x")));
        when(orgSettingRepository.findById("sla.deadline_hours.high"))
                .thenReturn(Optional.of(new OrgSettingEntity("sla.deadline_hours.high", "36")));
        when(orgSettingRepository.findById("sla.deadline_hours.critical"))
                .thenReturn(Optional.of(new OrgSettingEntity("sla.deadline_hours.critical", "0")));
        when(orgSettingRepository.findById("sla.warning_before_hours"))
                .thenReturn(Optional.of(new OrgSettingEntity("sla.warning_before_hours", "3")));

        TenantSettingsPort.TenantSettings settings = tenantService.getTenantSettings("acme");

        assertThat(settings.workflowDefaults().approvalTypeDefault()).isEqualTo(ApprovalType.LINEAR);
        assertThat(settings.workflowDefaults().requireDefaultApprovers()).isFalse();
        assertThat(settings.slaDefaults().lowHours()).isEqualTo(72);
        assertThat(settings.slaDefaults().mediumHours()).isEqualTo(48);
        assertThat(settings.slaDefaults().highHours()).isEqualTo(36);
        assertThat(settings.slaDefaults().criticalHours()).isEqualTo(8);
        assertThat(settings.slaDefaults().warningBeforeHours()).isEqualTo(3);
    }

    @Test
    void updateWorkflowDefaults_persistsBothWorkflowKeys() {
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(activeTenant("acme")));
        when(orgSettingRepository.save(any(OrgSettingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tenantService.updateWorkflowDefaults("acme", new TenantSettingsPort.WorkflowDefaults(ApprovalType.NON_LINEAR, false));

        org.mockito.ArgumentCaptor<OrgSettingEntity> captor = org.mockito.ArgumentCaptor.forClass(OrgSettingEntity.class);
        verify(orgSettingRepository, times(2)).save(captor.capture());
        Map<String, String> byKey = captor.getAllValues().stream()
                .collect(Collectors.toMap(OrgSettingEntity::getKey, OrgSettingEntity::getValue));

        assertThat(byKey).containsEntry("workflow.approval_type_default", "NON_LINEAR");
        assertThat(byKey).containsEntry("workflow.require_default_approvers", "false");
    }

    @Test
    void updateSlaDefaults_persistsAllSlaKeys() {
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(activeTenant("acme")));
        when(orgSettingRepository.save(any(OrgSettingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tenantService.updateSlaDefaults("acme", new TenantSettingsPort.SlaDefaults(96, 72, 36, 12, 3));

        org.mockito.ArgumentCaptor<OrgSettingEntity> captor = org.mockito.ArgumentCaptor.forClass(OrgSettingEntity.class);
        verify(orgSettingRepository, times(5)).save(captor.capture());
        Map<String, String> byKey = captor.getAllValues().stream()
                .collect(Collectors.toMap(OrgSettingEntity::getKey, OrgSettingEntity::getValue, (first, second) -> second));

        assertThat(byKey).containsEntry("sla.deadline_hours.low", "96");
        assertThat(byKey).containsEntry("sla.deadline_hours.medium", "72");
        assertThat(byKey).containsEntry("sla.deadline_hours.high", "36");
        assertThat(byKey).containsEntry("sla.deadline_hours.critical", "12");
        assertThat(byKey).containsEntry("sla.warning_before_hours", "3");
    }

    private TenantEntity activeTenant(String slug) {
        TenantEntity tenant = new TenantEntity("Acme Corp", slug);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }
}