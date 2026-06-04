package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.CompletionStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.domain.model.RiskLevel;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.AttachmentRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestCustomFieldRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeRequestWorkflowModeTest {

    @Mock ChangeRequestRepository changeRequestRepository;
    @Mock CrApproverRepository crApproverRepository;
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock ChangeRequestCustomFieldRepository customFieldRepository;
    @Mock ActivityStreamRepository activityStreamRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock UserRepository userRepository;
    @Mock OrgSettingRepository orgSettingRepository;
    @Mock RequestDeploymentService deploymentService;
    @Mock AuditLogService auditLogService;
    @Mock HtmlSanitizer htmlSanitizer;

    @InjectMocks ChangeRequestService changeRequestService;

    @BeforeEach
    void setUp() {
        lenient().when(htmlSanitizer.sanitize(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void completeRequest_approvalOnly_succeedsWhenApproved() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setStatus(ChangeRequestStatus.APPROVED);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(changeRequestRepository.save(cr)).thenReturn(cr);

        ChangeRequestEntity result = changeRequestService.completeRequest(crId, ownerId, "REQUESTER");

        assertThat(result.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);
    }

    @Test
    void completeRequest_approvalOnly_failsWhenDraft() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> changeRequestService.completeRequest(crId, ownerId, "REQUESTER"));
        assertThat(ex.getMessage()).contains("approved");
    }

    @Test
    void completeRequest_approvalOnly_failsWhenPendingApproval() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setApprovalStatus(ChangeRequestStatus.PENDING_APPROVAL);
        cr.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        assertThrows(InvalidStateTransitionException.class,
                () -> changeRequestService.completeRequest(crId, ownerId, "REQUESTER"));
    }

    @Test
    void completeRequest_approvalOnly_failsWhenRejected() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setApprovalStatus(ChangeRequestStatus.REJECTED);
        cr.setStatus(ChangeRequestStatus.REJECTED);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        assertThrows(InvalidStateTransitionException.class,
                () -> changeRequestService.completeRequest(crId, ownerId, "REQUESTER"));
    }

    @Test
    void completeRequest_deliveryPipeline_succeedsWhenApproved() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.DELIVERY_PIPELINE);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setStatus(ChangeRequestStatus.APPROVED);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(changeRequestRepository.save(cr)).thenReturn(cr);
        when(deploymentService.isDeploymentDone(crId)).thenReturn(true);

        ChangeRequestEntity result = changeRequestService.completeRequest(crId, ownerId, "REQUESTER");

        assertThat(result.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);
    }

    @Test
    void completeRequest_deliveryPipeline_failsWhenNotApproved() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.DELIVERY_PIPELINE);
        cr.setApprovalStatus(ChangeRequestStatus.PENDING_APPROVAL);
        cr.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        assertThrows(InvalidStateTransitionException.class,
                () -> changeRequestService.completeRequest(crId, ownerId, "REQUESTER"));
    }

    @Test
    void completeRequest_deliveryPipeline_failsWhenDeploymentNotDone() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.DELIVERY_PIPELINE);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setStatus(ChangeRequestStatus.APPROVED);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(deploymentService.isDeploymentDone(crId)).thenReturn(false);

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> changeRequestService.completeRequest(crId, ownerId, "REQUESTER"));
        assertThat(ex.getMessage()).containsIgnoringCase("deployment");
    }

    @Test
    void completeRequest_failsWhenAlreadyCompleted() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setStatus(ChangeRequestStatus.APPROVED);
        cr.setCompletionStatus(CompletionStatus.COMPLETED);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        assertThrows(InvalidStateTransitionException.class,
                () -> changeRequestService.completeRequest(crId, ownerId, "REQUESTER"));
    }

    @Test
    void completeRequest_failsForNonCreatorNonAdmin() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setStatus(ChangeRequestStatus.APPROVED);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        assertThrows(DomainNotPermittedException.class,
                () -> changeRequestService.completeRequest(crId, otherId, "REQUESTER"));
    }

    @Test
    void completeRequest_succeedsForAdmin() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setStatus(ChangeRequestStatus.APPROVED);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(changeRequestRepository.save(cr)).thenReturn(cr);

        ChangeRequestEntity result = changeRequestService.completeRequest(crId, adminId, "ADMIN");

        assertThat(result.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);
    }

    @Test
    void setWorkflowMode_succeedsInDraft() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(changeRequestRepository.save(cr)).thenReturn(cr);

        ChangeRequestEntity result = changeRequestService.setWorkflowMode(
                crId, RequestWorkflowMode.DELIVERY_PIPELINE, ownerId, "REQUESTER");

        assertThat(result.getWorkflowMode()).isEqualTo(RequestWorkflowMode.DELIVERY_PIPELINE);
    }

    @Test
    void setWorkflowMode_failsAfterSubmission() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        cr.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        cr.setApprovalStatus(ChangeRequestStatus.PENDING_APPROVAL);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        assertThrows(InvalidStateTransitionException.class,
                () -> changeRequestService.setWorkflowMode(
                        crId, RequestWorkflowMode.DELIVERY_PIPELINE, ownerId, "REQUESTER"));
    }

    @Test
    void setWorkflowMode_failsForNonCreatorNonAdmin() {
        UUID crId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        ChangeRequestEntity cr = buildCr(crId, ownerId, RequestWorkflowMode.APPROVAL_ONLY);
        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));

        assertThrows(DomainNotPermittedException.class,
                () -> changeRequestService.setWorkflowMode(
                        crId, RequestWorkflowMode.DELIVERY_PIPELINE, otherId, "REQUESTER"));
    }

    private ChangeRequestEntity buildCr(UUID crId, UUID ownerId, RequestWorkflowMode mode) {
        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", crId);
        cr.setTitle("Test CR");
        cr.setDescription("desc");
        cr.setPriority(Priority.MEDIUM);
        cr.setRiskLevel(RiskLevel.MEDIUM);
        cr.setApprovalType(ApprovalType.NON_LINEAR);
        cr.setCreatedBy(owner);
        cr.setWorkflowMode(mode);
        return cr;
    }
}
