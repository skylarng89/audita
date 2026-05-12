package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.AttachmentRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestCustomFieldRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceSecurityTest {

    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    CrApproverRepository crApproverRepository;
    @Mock
    ChangeRequestCustomFieldRepository customFieldRepository;
    @Mock
    ActivityStreamRepository activityStreamRepository;
    @Mock
    AttachmentRepository attachmentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    OrgSettingRepository orgSettingRepository;
    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    ChangeRequestService changeRequestService;

    @Test
    void updateDeniesRequesterWhoDoesNotOwnChangeRequest() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherRequesterId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));

        DomainNotPermittedException ex = assertThrows(
                DomainNotPermittedException.class,
                () -> tryUnauthorizedUpdate(changeRequestId, otherRequesterId));
        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");

        verifyNoInteractions(activityStreamRepository);
        verify(changeRequestRepository).findById(changeRequestId);
    }

    private void tryUnauthorizedUpdate(UUID changeRequestId, UUID requesterId) {
        changeRequestService.update(new ChangeRequestService.UpdateRequest(
                changeRequestId,
                "Updated by attacker",
                "should fail",
                Priority.HIGH,
                RiskLevel.HIGH,
                "Security",
                ApprovalType.LINEAR,
                OffsetDateTime.now().plusHours(2),
                OffsetDateTime.now().plusHours(3),
                List.of("db"),
                requesterId,
                "REQUESTER"));
    }

    @Test
    void submitDeniesRequesterWhoDoesNotOwnChangeRequest() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherRequesterId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));

        DomainNotPermittedException ex = assertThrows(
                DomainNotPermittedException.class,
                () -> changeRequestService.submit(changeRequestId, otherRequesterId, "REQUESTER"));
        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");

        verifyNoInteractions(activityStreamRepository);
        verify(changeRequestRepository).findById(changeRequestId);
    }

    @Test
    void submitUsesConfiguredSlaHoursForOwner() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);
        changeRequest.setPriority(Priority.HIGH);

        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId)).thenReturn(List.of());
        when(userRepository.findByRole_NameInAndStatusOrderByFullNameAsc(List.of("Approver", "Auditor"),
                UserStatus.ACTIVE))
                .thenReturn(List.of());
        when(orgSettingRepository.findById("sla.deadline_hours.high"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "sla.deadline_hours.high", "36")));
        when(changeRequestRepository.save(changeRequest)).thenReturn(changeRequest);

        OffsetDateTime before = OffsetDateTime.now();
        ChangeRequestEntity submitted = changeRequestService.submit(changeRequestId, ownerId, "REQUESTER");
        OffsetDateTime after = OffsetDateTime.now();

        assertThat(submitted.getStatus()).isEqualTo(io.audita.domain.model.ChangeRequestStatus.PENDING_APPROVAL);
        assertThat(submitted.getSlaDeadline()).isAfterOrEqualTo(before.plusHours(35));
        assertThat(submitted.getSlaDeadline()).isBeforeOrEqualTo(after.plusHours(37));
        verify(changeRequestRepository).save(changeRequest);
    }

    @Test
    void submitAutoAddsApproversAndAuditorsForOwner() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);

        UserEntity approverUser = new UserEntity("approver@example.com", "Approver User");
        ReflectionTestUtils.setField(approverUser, "id", UUID.randomUUID());
        approverUser.setStatus(UserStatus.ACTIVE);
        approverUser.setRole(new io.audita.infrastructure.persistence.entity.RoleEntity("Approver", ""));

        UserEntity auditorUser = new UserEntity("auditor@example.com", "Auditor User");
        ReflectionTestUtils.setField(auditorUser, "id", UUID.randomUUID());
        auditorUser.setStatus(UserStatus.ACTIVE);
        auditorUser.setRole(new io.audita.infrastructure.persistence.entity.RoleEntity("Auditor", ""));

        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId)).thenReturn(List.of());
        when(userRepository.findByRole_NameInAndStatusOrderByFullNameAsc(List.of("Approver", "Auditor"),
                UserStatus.ACTIVE))
                .thenReturn(List.of(approverUser, auditorUser));
        when(orgSettingRepository.findById(any())).thenReturn(Optional.empty());
        when(changeRequestRepository.save(changeRequest)).thenReturn(changeRequest);

        changeRequestService.submit(changeRequestId, ownerId, "REQUESTER");

        verify(crApproverRepository)
                .saveAll(org.mockito.ArgumentMatchers.argThat((List<CrApproverEntity> approvers) -> {
                    if (approvers.size() != 2) {
                        return false;
                    }
                    CrApproverEntity first = approvers.get(0);
                    CrApproverEntity second = approvers.get(1);
                    return first.getUser().getEmail().equals("approver@example.com")
                            && first.isRequired()
                            && first.getPosition() == 1
                            && second.getUser().getEmail().equals("auditor@example.com")
                            && !second.isRequired()
                            && second.getPosition() == 2;
                }));
    }

    private ChangeRequestEntity buildDraftChangeRequest(UUID ownerId) {
        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setTitle("Baseline CR");
        changeRequest.setDescription("baseline");
        changeRequest.setPriority(Priority.MEDIUM);
        changeRequest.setRiskLevel(RiskLevel.MEDIUM);
        changeRequest.setApprovalType(ApprovalType.LINEAR);
        changeRequest.setCreatedBy(owner);
        return changeRequest;
    }
}
