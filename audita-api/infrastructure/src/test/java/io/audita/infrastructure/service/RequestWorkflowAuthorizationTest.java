package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.CompletionStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.domain.model.RiskLevel;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.entity.OrgSettingEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.AttachmentRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestCustomFieldRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.CrWatcherRepository;
import io.audita.infrastructure.persistence.repository.GroupMemberRepository;
import io.audita.infrastructure.persistence.repository.GroupRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestUatCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatRepository;
import io.audita.infrastructure.persistence.repository.RequestUatWatcherRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.security.HtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestWorkflowAuthorizationTest {

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
    RequestUatRepository requestUatRepository;
    @Mock
    RequestUatApproverRepository requestUatApproverRepository;
    @Mock
    RequestUatCommentRepository requestUatCommentRepository;
    @Mock
    RequestUatWatcherRepository requestUatWatcherRepository;
    @Mock
    RequestDeploymentRepository deploymentRepository;
    @Mock
    RequestDeploymentCommentRepository deploymentCommentRepository;
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
    @Mock
    MentionNotifier mentionNotifier;

    @InjectMocks
    ChangeRequestService changeRequestService;

    @InjectMocks
    RequestUatService requestUatService;

    @InjectMocks
    RequestDeploymentService requestDeploymentService;

    @BeforeEach
    void setUp() {
        lenient().when(htmlSanitizer.sanitize(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Completion authorization matrix
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void creatorCanCompleteApprovalOnlyRequestWhenApproved() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(changeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChangeRequestEntity result = changeRequestService.completeRequest(requestId, creatorId, "REQUESTER");

        assertThat(result.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);
        verify(changeRequestRepository).save(cr);
    }

    @Test
    void adminCanCompleteAnyRequest() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(changeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChangeRequestEntity result = changeRequestService.completeRequest(requestId, adminId, "ADMIN");

        assertThat(result.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);
        verify(changeRequestRepository).save(cr);
    }

    @Test
    void nonCreatorRequesterCannotCompleteRequest() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID otherRequesterId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        DomainNotPermittedException ex = assertThrows(
                DomainNotPermittedException.class,
                () -> changeRequestService.completeRequest(requestId, otherRequesterId, "REQUESTER"));

        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void auditorCannotCompleteRequest() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID auditorId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        DomainNotPermittedException ex = assertThrows(
                DomainNotPermittedException.class,
                () -> changeRequestService.completeRequest(requestId, auditorId, "AUDITOR"));

        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void completeDeniedWhenApprovalOnlyRequestNotApproved() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        ChangeRequestEntity cr = buildDraftRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> changeRequestService.completeRequest(requestId, creatorId, "REQUESTER"));

        assertThat(ex.getMessage()).containsIgnoringCase("approved");
        verify(changeRequestRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UAT creation guards
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void uatCreationDeniedWhenRequestNotApproved() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        ChangeRequestEntity cr = buildDraftRequest(creatorId, RequestWorkflowMode.DELIVERY_PIPELINE);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.createUat(requestId, "UAT", "details", creatorId, "REQUESTER"));

        assertThat(ex.getMessage()).containsIgnoringCase("approved");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void uatCreationDeniedForApprovalOnlyMode() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.createUat(requestId, "UAT", "details", creatorId, "REQUESTER"));

        assertThat(ex.getMessage()).containsIgnoringCase("DELIVERY_PIPELINE");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void uatCreationDeniedForNonCreator() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.DELIVERY_PIPELINE);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        DomainNotPermittedException ex = assertThrows(
                DomainNotPermittedException.class,
                () -> requestUatService.createUat(requestId, "UAT", "details", otherUserId, "REQUESTER"));

        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
        verify(requestUatRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UAT promotion guards
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void uatPromotionDeniedWhenRequiredApproversPending() {
        UUID requestId = UUID.randomUUID();
        UUID uatId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.DELIVERY_PIPELINE);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        RequestUatEntity uat = new RequestUatEntity();
        ReflectionTestUtils.setField(uat, "id", uatId);
        uat.setRequestId(requestId);
        uat.setReadOnly(false);
        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));

        RequestUatApproverEntity pendingApprover = new RequestUatApproverEntity();
        ReflectionTestUtils.setField(pendingApprover, "id", UUID.randomUUID());
        pendingApprover.setUatId(uatId);
        pendingApprover.setUserId(approverId);
        pendingApprover.setStatus(ApproverStatus.PENDING);

        when(requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId))
                .thenReturn(List.of(pendingApprover));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.promoteToDeployment(uatId, creatorId, "REQUESTER"));

        assertThat(ex.getMessage()).containsIgnoringCase("required");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void uatPromotionDeniedForNonCreator() {
        UUID requestId = UUID.randomUUID();
        UUID uatId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        ChangeRequestEntity cr = buildApprovedRequest(creatorId, RequestWorkflowMode.DELIVERY_PIPELINE);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        RequestUatEntity uat = new RequestUatEntity();
        ReflectionTestUtils.setField(uat, "id", uatId);
        uat.setRequestId(requestId);
        uat.setReadOnly(false);
        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));

        DomainNotPermittedException ex = assertThrows(
                DomainNotPermittedException.class,
                () -> requestUatService.promoteToDeployment(uatId, otherUserId, "REQUESTER"));

        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
        verify(requestUatRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Workflow mode guards
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void workflowModeChangeDeniedAfterSubmission() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        ChangeRequestEntity cr = buildDraftRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> changeRequestService.setWorkflowMode(
                        requestId, RequestWorkflowMode.DELIVERY_PIPELINE, creatorId, "REQUESTER"));

        assertThat(ex.getMessage()).containsIgnoringCase("DRAFT");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void workflowModeChangeDeniedForNonCreator() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        ChangeRequestEntity cr = buildDraftRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        DomainNotPermittedException ex = assertThrows(
                DomainNotPermittedException.class,
                () -> changeRequestService.setWorkflowMode(
                        requestId, RequestWorkflowMode.DELIVERY_PIPELINE, otherUserId, "REQUESTER"));

        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void workflowModeChangeAllowedForCreatorInDraft() {
        UUID requestId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        ChangeRequestEntity cr = buildDraftRequest(creatorId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(changeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChangeRequestEntity result = changeRequestService.setWorkflowMode(
                requestId, RequestWorkflowMode.DELIVERY_PIPELINE, creatorId, "REQUESTER");

        assertThat(result.getWorkflowMode()).isEqualTo(RequestWorkflowMode.DELIVERY_PIPELINE);
        verify(changeRequestRepository).save(cr);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Display ID immutability
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void prefixChangeDoesNotMutateExistingDisplayIds() {
        UUID creatorId = UUID.randomUUID();
        UUID cr1Id = UUID.randomUUID();
        UUID cr2Id = UUID.randomUUID();

        UserEntity creator = new UserEntity("creator@example.com", "Creator");
        ReflectionTestUtils.setField(creator, "id", creatorId);

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        lenient().when(orgSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
        lenient().when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(cr1Id)).thenReturn(List.of());
        lenient().when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(cr2Id)).thenReturn(List.of());

        when(orgSettingRepository.findById("request.id_prefix"))
                .thenReturn(Optional.of(new OrgSettingEntity("request.id_prefix", "OLD")))
                .thenReturn(Optional.of(new OrgSettingEntity("request.id_prefix", "NEW")));
        when(orgSettingRepository.findById("request.id_sequence"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new OrgSettingEntity("request.id_sequence", "1")));
        lenient().when(orgSettingRepository.findById("workflow.default_approver_user_ids"))
                .thenReturn(Optional.empty());
        lenient().when(orgSettingRepository.findById("workflow.default_approver_group_ids"))
                .thenReturn(Optional.empty());

        ChangeRequestService.CreateRequest req = new ChangeRequestService.CreateRequest(
                "CR", "desc", Priority.MEDIUM, RiskLevel.MEDIUM,
                null, null, null, null, null, creatorId, null, null, null, null, null);

        ChangeRequestEntity first = changeRequestService.create(req);
        String firstDisplayId = first.getDisplayId();

        ChangeRequestEntity second = changeRequestService.create(req);

        assertThat(firstDisplayId).isEqualTo("OLD-000001");
        assertThat(first.getDisplayId()).isEqualTo("OLD-000001");
        assertThat(second.getDisplayId()).isEqualTo("NEW-000002");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private ChangeRequestEntity buildDraftRequest(UUID creatorId, RequestWorkflowMode mode) {
        UserEntity creator = new UserEntity("creator@example.com", "Creator");
        ReflectionTestUtils.setField(creator, "id", creatorId);

        ChangeRequestEntity cr = new ChangeRequestEntity();
        cr.setTitle("Test Request");
        cr.setDescription("Test");
        cr.setPriority(Priority.MEDIUM);
        cr.setRiskLevel(RiskLevel.MEDIUM);
        cr.setApprovalType(ApprovalType.NON_LINEAR);
        cr.setCreatedBy(creator);
        cr.setWorkflowMode(mode);
        cr.setStatus(ChangeRequestStatus.DRAFT);
        cr.setApprovalStatus(ChangeRequestStatus.DRAFT);
        return cr;
    }

    private ChangeRequestEntity buildApprovedRequest(UUID creatorId, RequestWorkflowMode mode) {
        ChangeRequestEntity cr = buildDraftRequest(creatorId, mode);
        cr.setStatus(ChangeRequestStatus.APPROVED);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        return cr;
    }
}
