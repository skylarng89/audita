package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
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
import io.audita.infrastructure.persistence.repository.CrWatcherRepository;
import io.audita.infrastructure.persistence.repository.GroupMemberRepository;
import io.audita.infrastructure.persistence.repository.GroupRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.security.HtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceSecurityTest {

    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    CrApproverRepository crApproverRepository;
    @Mock
    CrWatcherRepository crWatcherRepository;
    @Mock
    GroupRepository groupRepository;
    @Mock
    GroupMemberRepository groupMemberRepository;
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
    RequestDeploymentService deploymentService;
    @Mock
    AuditLogService auditLogService;
    @Mock
    HtmlSanitizer htmlSanitizer;
    @Mock
    NotificationService notificationService;
    @Mock
    EmailService emailService;

    @InjectMocks
    ChangeRequestService changeRequestService;

    @BeforeEach
    void setUp() {
        lenient().when(htmlSanitizer.sanitize(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listGrantsGlobalVisibilityForAuditor() {
        Pageable pageable = PageRequest.of(0, 20);
        when(changeRequestRepository.findAllFiltered(any(), any(), any(), any(), any(), eq(true), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        changeRequestService.list(null, null, null, null, UUID.randomUUID(), "AUDITOR", pageable);

        verify(changeRequestRepository).findAllFiltered(any(), any(), any(), any(), any(), eq(true), any(), eq(pageable));
    }

    @Test
    void listRestrictsVisibilityForRequester() {
        Pageable pageable = PageRequest.of(0, 20);
        when(changeRequestRepository.findAllFiltered(any(), any(), any(), any(), any(), eq(false), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        changeRequestService.list(null, null, null, null, UUID.randomUUID(), "REQUESTER", pageable);

        verify(changeRequestRepository).findAllFiltered(any(), any(), any(), any(), any(), eq(false), any(), eq(pageable));
    }

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
                "REQUESTER",
                null,
                null,
                null,
                null,
                null));
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
    void submitRequiresAtLeastOneApprover() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);
        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(crApproverRepository.countByChangeRequestId(changeRequestId)).thenReturn(0);

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> changeRequestService.submit(changeRequestId, ownerId, "REQUESTER"));

        assertThat(ex.getErrorCode()).isEqualTo("APPROVERS_REQUIRED");
        verifyNoInteractions(activityStreamRepository);
    }

    @Test
    void submitUsesConfiguredSlaHoursForOwner() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);
        changeRequest.setPriority(Priority.HIGH);

        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_user_ids", "")));
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_group_ids", "")));
        when(crApproverRepository.countByChangeRequestId(changeRequestId)).thenReturn(1);
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
    void submitAutoAddsApproversAuditorsAndAdminsForOwner() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);

        UserEntity approverUser = new UserEntity("approver@example.com", "Approver User");
        UUID configuredUserId = UUID.randomUUID();
        ReflectionTestUtils.setField(approverUser, "id", configuredUserId);
        approverUser.setStatus(UserStatus.ACTIVE);
        approverUser.setRole(new io.audita.infrastructure.persistence.entity.RoleEntity("Approver", ""));

        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId)).thenReturn(List.of());
        when(userRepository.findByIdInAndStatusOrderByFullNameAsc(List.of(configuredUserId), UserStatus.ACTIVE))
                .thenReturn(List.of(approverUser));
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_user_ids", configuredUserId.toString())));
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_group_ids", "")));
        when(crApproverRepository.countByChangeRequestId(changeRequestId)).thenReturn(1);
        when(changeRequestRepository.save(changeRequest)).thenReturn(changeRequest);

        changeRequestService.submit(changeRequestId, ownerId, "REQUESTER");

        verify(crApproverRepository)
                .saveAll(org.mockito.ArgumentMatchers.argThat((List<CrApproverEntity> approvers) -> {
                    if (approvers.size() != 1) {
                        return false;
                    }
                    CrApproverEntity first = approvers.get(0);
                    return first.getUser().getEmail().equals("approver@example.com")
                            && first.getPosition() == 1;
                }));
    }

    @Test
    void createAutoAddsApproversAuditorsAndAdmins() {
        UUID ownerId = UUID.randomUUID();
        UUID changeRequestId = UUID.randomUUID();

        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        UserEntity approverUser = new UserEntity("approver@example.com", "Approver User");
        UUID configuredUserId = UUID.randomUUID();
        ReflectionTestUtils.setField(approverUser, "id", configuredUserId);
        approverUser.setStatus(UserStatus.ACTIVE);
        approverUser.setRole(new io.audita.infrastructure.persistence.entity.RoleEntity("Approver", ""));

        ChangeRequestEntity created = new ChangeRequestEntity();
        ReflectionTestUtils.setField(created, "id", changeRequestId);
        created.setTitle("Created CR");
        created.setDescription("desc");
        created.setPriority(Priority.HIGH);
        created.setRiskLevel(RiskLevel.HIGH);
        created.setApprovalType(ApprovalType.NON_LINEAR);
        created.setCreatedBy(owner);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(orgSettingRepository.findById("request.id_prefix")).thenReturn(Optional.empty());
        when(orgSettingRepository.findById("request.id_sequence")).thenReturn(Optional.empty());
        when(orgSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeRequestRepository.save(any(ChangeRequestEntity.class))).thenReturn(created);
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId)).thenReturn(List.of());
        when(userRepository.findByIdInAndStatusOrderByFullNameAsc(List.of(configuredUserId), UserStatus.ACTIVE))
                .thenReturn(List.of(approverUser));
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_user_ids", configuredUserId.toString())));
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_group_ids", "")));

        changeRequestService.create(new ChangeRequestService.CreateRequest(
                "Created CR",
                "desc",
                Priority.HIGH,
                RiskLevel.HIGH,
                "Security",
                ApprovalType.NON_LINEAR,
                null,
                null,
                List.of("db"),
                ownerId,
                null,
                null,
                null,
                null,
                null));

        verify(crApproverRepository)
                .saveAll(org.mockito.ArgumentMatchers
                        .argThat((List<CrApproverEntity> approvers) -> approvers.size() == 1));
    }

    @Test
    void createAutoAddsConfiguredApproversEvenWhenRequireDefaultApproversDisabled() {
        UUID ownerId = UUID.randomUUID();
        UUID changeRequestId = UUID.randomUUID();
        UUID configuredUserId = UUID.randomUUID();

        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        UserEntity approverUser = new UserEntity("approver@example.com", "Approver User");
        ReflectionTestUtils.setField(approverUser, "id", configuredUserId);
        approverUser.setStatus(UserStatus.ACTIVE);

        ChangeRequestEntity created = new ChangeRequestEntity();
        ReflectionTestUtils.setField(created, "id", changeRequestId);
        created.setTitle("Created CR");
        created.setDescription("desc");
        created.setPriority(Priority.HIGH);
        created.setRiskLevel(RiskLevel.HIGH);
        created.setApprovalType(ApprovalType.NON_LINEAR);
        created.setCreatedBy(owner);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(orgSettingRepository.findById("request.id_prefix")).thenReturn(Optional.empty());
        when(orgSettingRepository.findById("request.id_sequence")).thenReturn(Optional.empty());
        when(orgSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeRequestRepository.save(any(ChangeRequestEntity.class))).thenReturn(created);
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId)).thenReturn(List.of());
        when(userRepository.findByIdInAndStatusOrderByFullNameAsc(List.of(configuredUserId), UserStatus.ACTIVE))
                .thenReturn(List.of(approverUser));
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_user_ids", configuredUserId.toString())));
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "workflow.default_approver_group_ids", "")));

        changeRequestService.create(new ChangeRequestService.CreateRequest(
                "Created CR",
                "desc",
                Priority.HIGH,
                RiskLevel.HIGH,
                "Security",
                ApprovalType.NON_LINEAR,
                null,
                null,
                List.of("db"),
                ownerId,
                null,
                null,
                null,
                null,
                null));

        verify(crApproverRepository)
                .saveAll(org.mockito.ArgumentMatchers.argThat((List<CrApproverEntity> approvers) -> {
                    if (approvers.size() != 1) {
                        return false;
                    }
                    CrApproverEntity first = approvers.get(0);
                    return first.getUser().getId().equals(configuredUserId)
                            && first.getPosition() == 1;
                }));
    }

    @Test
    void removeApproverAllowsPendingApprovalWhenTargetHasNotVoted() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        changeRequest.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        changeRequest.setApprovalLocked(true);
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);

        UserEntity pendingUser = new UserEntity("pending@example.com", "Pending User");
        ReflectionTestUtils.setField(pendingUser, "id", UUID.randomUUID());
        CrApproverEntity approver = new CrApproverEntity(changeRequest, pendingUser, 1);
        ReflectionTestUtils.setField(approver, "id", approverId);
        approver.setStatus(ApproverStatus.PENDING);

        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(crApproverRepository.findById(approverId)).thenReturn(Optional.of(approver));
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId)).thenReturn(List.of());

        changeRequestService.removeApprover(changeRequestId, approverId, ownerId, "REQUESTER");

        verify(crApproverRepository).delete(approver);
        verify(crApproverRepository).saveAll(any());
    }

    @Test
    void removeApproverRejectsWhenApproverAlreadyVoted() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        changeRequest.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        changeRequest.setApprovalLocked(true);
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);

        UserEntity decidedUser = new UserEntity("decided@example.com", "Decided User");
        ReflectionTestUtils.setField(decidedUser, "id", UUID.randomUUID());
        CrApproverEntity approver = new CrApproverEntity(changeRequest, decidedUser, 1);
        ReflectionTestUtils.setField(approver, "id", approverId);
        approver.setStatus(ApproverStatus.APPROVED);

        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(crApproverRepository.findById(approverId)).thenReturn(Optional.of(approver));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> changeRequestService.removeApprover(changeRequestId, approverId, ownerId, "REQUESTER"));

        assertThat(ex.getErrorCode()).isEqualTo("APPROVER_DECISION_LOCKED");
        verify(crApproverRepository, never()).delete(any());
    }

    @Test
    void createAssignsDefaultDisplayIdWhenNoPrefixSetting() {
        UUID ownerId = UUID.randomUUID();
        UUID changeRequestId = UUID.randomUUID();

        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(orgSettingRepository.findById("request.id_prefix")).thenReturn(Optional.empty());
        when(orgSettingRepository.findById("request.id_sequence")).thenReturn(Optional.empty());
        when(orgSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeRequestRepository.save(any(ChangeRequestEntity.class))).thenAnswer(inv -> {
            ChangeRequestEntity cr = inv.getArgument(0);
            ReflectionTestUtils.setField(cr, "id", changeRequestId);
            return cr;
        });
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.empty());
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.empty());

        ChangeRequestEntity result = changeRequestService.create(new ChangeRequestService.CreateRequest(
                "Test CR", "desc", Priority.MEDIUM, RiskLevel.MEDIUM,
                null, null, null, null, null, ownerId, null, null, null, null, null));

        assertThat(result.getDisplayId()).isEqualTo("RQ-000001");
    }

    @Test
    void createUsesCustomPrefixFromOrgSettings() {
        UUID ownerId = UUID.randomUUID();
        UUID changeRequestId = UUID.randomUUID();

        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(orgSettingRepository.findById("request.id_prefix"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "request.id_prefix", "IT")));
        when(orgSettingRepository.findById("request.id_sequence")).thenReturn(Optional.empty());
        when(orgSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeRequestRepository.save(any(ChangeRequestEntity.class))).thenAnswer(inv -> {
            ChangeRequestEntity cr = inv.getArgument(0);
            ReflectionTestUtils.setField(cr, "id", changeRequestId);
            return cr;
        });
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.empty());
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.empty());

        ChangeRequestEntity result = changeRequestService.create(new ChangeRequestService.CreateRequest(
                "Test CR", "desc", Priority.MEDIUM, RiskLevel.MEDIUM,
                null, null, null, null, null, ownerId, null, null, null, null, null));

        assertThat(result.getDisplayId()).isEqualTo("IT-000001");
    }

    @Test
    void createIncrementsSequenceOnEachCreate() {
        UUID ownerId = UUID.randomUUID();
        UUID cr1Id = UUID.randomUUID();
        UUID cr2Id = UUID.randomUUID();

        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(orgSettingRepository.findById("request.id_prefix")).thenReturn(Optional.empty());
        when(orgSettingRepository.findById("request.id_sequence"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "request.id_sequence", "1")));
        when(orgSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeRequestRepository.save(any(ChangeRequestEntity.class)))
                .thenAnswer(inv -> {
                    ChangeRequestEntity cr = inv.getArgument(0);
                    ReflectionTestUtils.setField(cr, "id", cr1Id);
                    return cr;
                })
                .thenAnswer(inv -> {
                    ChangeRequestEntity cr = inv.getArgument(0);
                    ReflectionTestUtils.setField(cr, "id", cr2Id);
                    return cr;
                });
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.empty());
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.empty());

        ChangeRequestService.CreateRequest req = new ChangeRequestService.CreateRequest(
                "CR1", "desc", Priority.MEDIUM, RiskLevel.MEDIUM,
                null, null, null, null, null, ownerId, null, null, null, null, null);

        ChangeRequestEntity first = changeRequestService.create(req);
        ChangeRequestEntity second = changeRequestService.create(req);

        assertThat(first.getDisplayId()).isEqualTo("RQ-000001");
        assertThat(second.getDisplayId()).isEqualTo("RQ-000002");
    }

    @Test
    void createDoesNotMutateExistingDisplayIdWhenPrefixChanges() {
        UUID ownerId = UUID.randomUUID();
        UUID cr1Id = UUID.randomUUID();
        UUID cr2Id = UUID.randomUUID();

        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(orgSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeRequestRepository.save(any(ChangeRequestEntity.class)))
                .thenAnswer(inv -> {
                    ChangeRequestEntity cr = inv.getArgument(0);
                    ReflectionTestUtils.setField(cr, "id", cr1Id);
                    return cr;
                })
                .thenAnswer(inv -> {
                    ChangeRequestEntity cr = inv.getArgument(0);
                    ReflectionTestUtils.setField(cr, "id", cr2Id);
                    return cr;
                });
        when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.empty());
        when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.empty());

        when(orgSettingRepository.findById("request.id_prefix"))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "request.id_prefix", "OLD")))
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "request.id_prefix", "NEW")));
        when(orgSettingRepository.findById("request.id_sequence"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new io.audita.infrastructure.persistence.entity.OrgSettingEntity(
                        "request.id_sequence", "1")));

        ChangeRequestService.CreateRequest req = new ChangeRequestService.CreateRequest(
                "CR", "desc", Priority.MEDIUM, RiskLevel.MEDIUM,
                null, null, null, null, null, ownerId, null, null, null, null, null);

        ChangeRequestEntity first = changeRequestService.create(req);
        String firstDisplayId = first.getDisplayId();

        ChangeRequestEntity second = changeRequestService.create(req);

        assertThat(firstDisplayId).isEqualTo("OLD-000001");
        assertThat(first.getDisplayId()).isEqualTo("OLD-000001");
        assertThat(second.getDisplayId()).isEqualTo("NEW-000002");
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
