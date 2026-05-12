package io.audita.infrastructure.service;

import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import io.audita.domain.model.TenantStatus;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.entity.OrgSettingEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaMonitoringServiceTest {

    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    CrApproverRepository crApproverRepository;
    @Mock
    ActivityStreamRepository activityStreamRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    EmailService emailService;
    @Mock
    TenantRepository tenantRepository;
    @Mock
    OrgSettingRepository orgSettingRepository;
    @Mock
    PlatformTransactionManager transactionManager;
    @Mock
    TransactionStatus transactionStatus;

    @InjectMocks
    SlaMonitoringService slaMonitoringService;

    @Test
    void evaluate_emits_warning_notifications_without_breach_email() {
        ChangeRequestEntity cr = warningCr();
        UserEntity creator = cr.getCreatedBy();
        UserEntity approverUser = user("approver@example.com", "Approver Two");
        TenantEntity tenant = activeTenant();

        CrApproverEntity approver = new CrApproverEntity(cr, approverUser, true, 1, false);
        when(tenantRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(tenant));
        when(orgSettingRepository.findById("sla.warning_before_hours")).thenReturn(Optional.empty());
        when(changeRequestRepository.findSlaWarning(any(), any())).thenReturn(List.of(cr));
        when(changeRequestRepository.findSlaBreached(any())).thenReturn(List.of());
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(cr.getId())).thenReturn(List.of(approver));

        slaMonitoringService.evaluate();

        verify(activityStreamRepository).save(any());
        verify(notificationService).createAndPush(eq(creator.getId()), eq("SLA_WARNING"), any(), any(), any());
        verify(notificationService).createAndPush(eq(approverUser.getId()), eq("SLA_WARNING"), any(), any(), any());
    }

    @Test
    void evaluate_marks_breach_and_sends_breach_email() {
        ChangeRequestEntity cr = warningCr();
        UserEntity creator = cr.getCreatedBy();
        TenantEntity tenant = activeTenant();

        when(tenantRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(tenant));
        when(orgSettingRepository.findById("sla.warning_before_hours")).thenReturn(Optional.empty());
        when(changeRequestRepository.findSlaWarning(any(), any())).thenReturn(List.of());
        when(changeRequestRepository.findSlaBreached(any())).thenReturn(List.of(cr));
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(cr.getId())).thenReturn(List.of());

        slaMonitoringService.evaluate();

        verify(changeRequestRepository).save(cr);
        verify(notificationService).createAndPush(eq(creator.getId()), eq("SLA_BREACH"), any(), any(), any());
        verify(emailService).sendSlaBreachEmail(
                creator.getEmail(),
                creator.getFullName(),
                cr.getTitle(),
                cr.getId().toString());
    }

    @Test
    void evaluate_uses_configured_warning_hours_window() {
        TenantEntity tenant = activeTenant();
        ChangeRequestEntity cr = warningCr();

        when(tenantRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(tenant));
        when(orgSettingRepository.findById("sla.warning_before_hours"))
                .thenReturn(Optional.of(new OrgSettingEntity("sla.warning_before_hours", "3")));
        when(changeRequestRepository.findSlaWarning(any(), any())).thenReturn(List.of(cr));
        when(changeRequestRepository.findSlaBreached(any())).thenReturn(List.of());
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(cr.getId())).thenReturn(List.of());

        slaMonitoringService.evaluate();

        ArgumentCaptor<OffsetDateTime> nowCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> warningCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(changeRequestRepository).findSlaWarning(nowCaptor.capture(), warningCaptor.capture());

        long hours = Duration.between(nowCaptor.getValue(), warningCaptor.getValue()).toHours();
        assertThat(hours).isEqualTo(3);
    }

    private ChangeRequestEntity warningCr() {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", UUID.randomUUID());
        cr.setTitle("Firewall update");
        cr.setPriority(Priority.HIGH);
        cr.setRiskLevel(RiskLevel.MEDIUM);
        cr.setApprovalType(ApprovalType.LINEAR);
        cr.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        cr.setSlaDeadline(OffsetDateTime.now().plusMinutes(30));
        cr.setCreatedBy(user("creator@example.com", "Creator One"));
        return cr;
    }

    private UserEntity user(String email, String fullName) {
        UserEntity user = new UserEntity(email, fullName);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private TenantEntity activeTenant() {
        TenantEntity tenant = new TenantEntity("Acme Corp", "acme");
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }
}
