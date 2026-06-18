package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestUatApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestUatCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatRepository;
import io.audita.infrastructure.persistence.repository.RequestUatWatcherRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestUatServiceTest {

    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    RequestUatRepository requestUatRepository;
    @Mock
    RequestUatApproverRepository requestUatApproverRepository;
    @Mock
    RequestUatCommentRepository requestUatCommentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    RequestDeploymentService deploymentService;
    @Mock
    AuditLogService auditLogService;
    @Mock
    ActivityStreamRepository activityStreamRepository;
    @Mock
    RequestUatWatcherRepository requestUatWatcherRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    EmailService emailService;
    @Mock
    MentionNotifier mentionNotifier;

    @InjectMocks
    RequestUatService requestUatService;

    @Test
    void createUatSucceedsWhenRequestApprovedAndDeliveryPipeline() {
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(requestUatRepository.existsByRequestId(requestId)).thenReturn(false);
        when(requestUatRepository.save(any(RequestUatEntity.class))).thenAnswer(inv -> {
            RequestUatEntity uat = inv.getArgument(0);
            ReflectionTestUtils.setField(uat, "id", UUID.randomUUID());
            return uat;
        });

        RequestUatEntity result = requestUatService.createUat(requestId, "UAT Plan", "details", actorId, "ADMIN");

        assertThat(result.getTitle()).isEqualTo("UAT Plan");
        assertThat(result.getDetails()).isEqualTo("details");
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(result.isReadOnly()).isFalse();
        verify(requestUatRepository).save(any(RequestUatEntity.class));
    }

    @Test
    void createUatFailsWhenRequestNotApproved() {
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setApprovalStatus(ChangeRequestStatus.PENDING_APPROVAL);
        cr.setWorkflowMode(RequestWorkflowMode.DELIVERY_PIPELINE);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.createUat(requestId, "UAT", "d", actorId, "ADMIN"));

        assertThat(ex.getMessage()).contains("approved");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void createUatFailsWhenApprovalOnlyMode() {
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ChangeRequestEntity cr = buildApprovedRequest(requestId, RequestWorkflowMode.APPROVAL_ONLY);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.createUat(requestId, "UAT", "d", actorId, "ADMIN"));

        assertThat(ex.getMessage()).containsIgnoringCase("delivery");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void createUatFailsWhenUatAlreadyExists() {
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(requestUatRepository.existsByRequestId(requestId)).thenReturn(true);

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.createUat(requestId, "UAT", "d", actorId, "ADMIN"));

        assertThat(ex.getMessage()).containsIgnoringCase("already");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void updateUatSucceedsWhenNotReadOnly() {
        UUID uatId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RequestUatEntity uat = buildInProgressUat(uatId, requestId);
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(requestUatRepository.save(any(RequestUatEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestUatEntity result = requestUatService.updateUat(uatId, "Updated", "new details", actorId, "ADMIN");

        assertThat(result.getTitle()).isEqualTo("Updated");
        assertThat(result.getDetails()).isEqualTo("new details");
    }

    @Test
    void updateUatFailsWhenReadOnly() {
        UUID uatId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RequestUatEntity uat = buildInProgressUat(uatId, requestId);
        uat.setReadOnly(true);
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.updateUat(uatId, "X", "y", actorId, "ADMIN"));

        assertThat(ex.getMessage()).containsIgnoringCase("read");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void promoteSucceedsWhenAllRequiredApproversApproved() {
        UUID uatId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RequestUatEntity uat = buildInProgressUat(uatId, requestId);
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        RequestUatApproverEntity approver = buildApprovedApprover(uatId, true);

        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId)).thenReturn(List.of(approver));
        when(requestUatRepository.save(any(RequestUatEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestUatEntity result = requestUatService.promoteToDeployment(uatId, actorId, "ADMIN");

        assertThat(result.isReadOnly()).isTrue();
        assertThat(result.getStatus()).isEqualTo("PROMOTED");
    }

    @Test
    void promoteFailsWhenRequiredApproverPending() {
        UUID uatId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RequestUatEntity uat = buildInProgressUat(uatId, requestId);
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        RequestUatApproverEntity pending = new RequestUatApproverEntity();
        ReflectionTestUtils.setField(pending, "id", UUID.randomUUID());
        pending.setUatId(uatId);
        pending.setUserId(UUID.randomUUID());
        pending.setPosition(1);
        pending.setStatus(ApproverStatus.PENDING);

        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId)).thenReturn(List.of(pending));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> requestUatService.promoteToDeployment(uatId, actorId, "ADMIN"));

        assertThat(ex.getMessage()).containsIgnoringCase("approver");
        verify(requestUatRepository, never()).save(any());
    }

    @Test
    void promoteSetsReadOnlyTrueAndStatusPromoted() {
        UUID uatId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RequestUatEntity uat = buildInProgressUat(uatId, requestId);
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        RequestUatApproverEntity a1 = buildApprovedApprover(uatId, true);
        RequestUatApproverEntity a2 = buildApprovedApprover(uatId, false);

        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId)).thenReturn(List.of(a1, a2));
        when(requestUatRepository.save(any(RequestUatEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestUatEntity result = requestUatService.promoteToDeployment(uatId, actorId, "ADMIN");

        assertThat(result.isReadOnly()).isTrue();
        assertThat(result.getStatus()).isEqualTo("PROMOTED");
        verify(requestUatRepository).save(uat);
    }

    private ChangeRequestEntity buildApprovedDeliveryPipelineRequest(UUID requestId) {
        return buildApprovedRequest(requestId, RequestWorkflowMode.DELIVERY_PIPELINE);
    }

    private ChangeRequestEntity buildApprovedRequest(UUID requestId, RequestWorkflowMode mode) {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setWorkflowMode(mode);
        return cr;
    }

    private RequestUatEntity buildInProgressUat(UUID uatId, UUID requestId) {
        RequestUatEntity uat = new RequestUatEntity();
        ReflectionTestUtils.setField(uat, "id", uatId);
        uat.setRequestId(requestId);
        uat.setTitle("UAT");
        uat.setDetails("details");
        uat.setStatus("IN_PROGRESS");
        uat.setReadOnly(false);
        uat.setCreatedBy(UUID.randomUUID());
        return uat;
    }

    private RequestUatApproverEntity buildApprovedApprover(UUID uatId, boolean required) {
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        ReflectionTestUtils.setField(approver, "id", UUID.randomUUID());
        approver.setUatId(uatId);
        approver.setUserId(UUID.randomUUID());
        approver.setPosition(1);
        approver.setStatus(ApproverStatus.APPROVED);
        return approver;
    }
}
