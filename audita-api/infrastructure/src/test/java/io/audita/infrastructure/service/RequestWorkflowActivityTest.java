package io.audita.infrastructure.service;

import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestUatCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatRepository;
import io.audita.infrastructure.persistence.repository.RequestUatWatcherRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestWorkflowActivityTest {

    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    RequestUatRepository requestUatRepository;
    @Mock
    RequestUatApproverRepository requestUatApproverRepository;
    @Mock
    RequestUatWatcherRepository requestUatWatcherRepository;
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
    RequestDeploymentRepository deploymentRepository;
    @Mock
    RequestDeploymentCommentRepository deploymentCommentRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    EmailService emailService;
    @Mock
    MentionNotifier mentionNotifier;

    RequestUatService requestUatService;
    RequestDeploymentService requestDeploymentService;

    @BeforeEach
    void setUp() {
        requestUatService = new RequestUatService(
                changeRequestRepository,
                requestUatRepository,
                requestUatApproverRepository,
                requestUatWatcherRepository,
                requestUatCommentRepository,
                userRepository,
                deploymentService,
                auditLogService,
                activityStreamRepository,
                notificationService,
                emailService,
                mentionNotifier
        );

        requestDeploymentService = new RequestDeploymentService(
                deploymentRepository,
                deploymentCommentRepository,
                userRepository,
                auditLogService,
                activityStreamRepository,
                changeRequestRepository,
                notificationService,
                mentionNotifier
        );
    }

    @Test
    void createUatEmitsActivityEvent() {
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

        requestUatService.createUat(requestId, "UAT Plan", "details", actorId, "ADMIN");

        ArgumentCaptor<ActivityStreamEntity> captor = ArgumentCaptor.forClass(ActivityStreamEntity.class);
        verify(activityStreamRepository).save(captor.capture());

        ActivityStreamEntity activity = captor.getValue();
        assertThat(activity.getActionType()).isEqualTo("UAT_CREATED");
        assertThat(activity.getChangeRequest()).isEqualTo(cr);
        assertThat(activity.getPayload()).containsEntry("requestId", requestId.toString());
        assertThat(activity.getPayload()).containsEntry("title", "UAT Plan");
    }

    @Test
    void promoteUatEmitsActivityEvent() {
        UUID uatId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RequestUatEntity uat = buildInProgressUat(uatId, requestId);
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);
        RequestUatApproverEntity approver = buildApprover(uatId, actorId);
        approver.approve();

        when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId))
                .thenReturn(List.of(approver));
        when(requestUatRepository.save(any(RequestUatEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        requestUatService.promoteToDeployment(uatId, actorId, "ADMIN");

        ArgumentCaptor<ActivityStreamEntity> captor = ArgumentCaptor.forClass(ActivityStreamEntity.class);
        verify(activityStreamRepository).save(captor.capture());

        ActivityStreamEntity activity = captor.getValue();
        assertThat(activity.getActionType()).isEqualTo("UAT_PROMOTED");
        assertThat(activity.getChangeRequest()).isEqualTo(cr);
        assertThat(activity.getPayload()).containsEntry("requestId", requestId.toString());
    }

    @Test
    void createDeploymentEmitsActivityEvent() {
        UUID requestId = UUID.randomUUID();
        UUID uatId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(deploymentRepository.existsByRequestId(requestId)).thenReturn(false);
        when(deploymentRepository.save(any(RequestDeploymentEntity.class))).thenAnswer(inv -> {
            RequestDeploymentEntity d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });

        requestDeploymentService.createFromPromotion(requestId, uatId, actorId);

        ArgumentCaptor<ActivityStreamEntity> captor = ArgumentCaptor.forClass(ActivityStreamEntity.class);
        verify(activityStreamRepository).save(captor.capture());

        ActivityStreamEntity activity = captor.getValue();
        assertThat(activity.getActionType()).isEqualTo("DEPLOYMENT_CREATED");
        assertThat(activity.getChangeRequest()).isEqualTo(cr);
        assertThat(activity.getPayload()).containsEntry("requestId", requestId.toString());
        assertThat(activity.getPayload()).containsEntry("uatId", uatId.toString());
    }

    private ChangeRequestEntity buildApprovedDeliveryPipelineRequest(UUID requestId) {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setWorkflowMode(RequestWorkflowMode.DELIVERY_PIPELINE);
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
        return uat;
    }

    private RequestUatApproverEntity buildApprover(UUID uatId, UUID userId) {
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        ReflectionTestUtils.setField(approver, "id", UUID.randomUUID());
        approver.setUatId(uatId);
        approver.setUserId(userId);
        approver.setPosition(1);
        return approver;
    }
}
