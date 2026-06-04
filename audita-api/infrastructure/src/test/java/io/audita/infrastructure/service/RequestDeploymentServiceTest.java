package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApproverStatus;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatApproverRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class RequestDeploymentServiceTest {

    @Mock
    RequestDeploymentRepository deploymentRepository;
    @Mock
    RequestDeploymentApproverRepository deploymentApproverRepository;
    @Mock
    RequestDeploymentCommentRepository deploymentCommentRepository;
    @Mock
    CrApproverRepository crApproverRepository;
    @Mock
    RequestUatApproverRepository uatApproverRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    AuditLogService auditLogService;
    @Mock
    ActivityStreamRepository activityStreamRepository;

    @InjectMocks
    RequestDeploymentService deploymentService;

    @Test
    void createFromPromotionCreatesDeploymentWithCorrectRequestAndUatId() {
        UUID requestId = UUID.randomUUID();
        UUID uatId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(deploymentRepository.existsByRequestId(requestId)).thenReturn(false);
        when(deploymentRepository.save(any(RequestDeploymentEntity.class))).thenAnswer(inv -> {
            RequestDeploymentEntity d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });
        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(requestId)).thenReturn(List.of());
        when(uatApproverRepository.findByUatIdOrderByPositionAsc(uatId)).thenReturn(List.of());

        RequestDeploymentEntity result = deploymentService.createFromPromotion(requestId, uatId, actorId);

        assertThat(result.getRequestId()).isEqualTo(requestId);
        assertThat(result.getUatId()).isEqualTo(uatId);
        assertThat(result.getCreatedBy()).isEqualTo(actorId);
        assertThat(result.getStatus()).isEqualTo("PENDING_APPROVAL");
        verify(deploymentRepository).save(any(RequestDeploymentEntity.class));
    }

    @Test
    void createFromPromotionInheritsApproversDeduplicated() {
        UUID requestId = UUID.randomUUID();
        UUID uatId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID sharedUserId = UUID.randomUUID();
        UUID crOnlyUserId = UUID.randomUUID();
        UUID uatOnlyUserId = UUID.randomUUID();

        when(deploymentRepository.existsByRequestId(requestId)).thenReturn(false);
        when(deploymentRepository.save(any(RequestDeploymentEntity.class))).thenAnswer(inv -> {
            RequestDeploymentEntity d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });

        UserEntity sharedUser = buildUser(sharedUserId);
        UserEntity crOnlyUser = buildUser(crOnlyUserId);

        CrApproverEntity crApprover1 = buildCrApprover(requestId, sharedUser, true, 1);
        CrApproverEntity crApprover2 = buildCrApprover(requestId, crOnlyUser, false, 2);

        RequestUatApproverEntity uatApprover1 = buildUatApprover(uatId, sharedUserId, false, 1);
        RequestUatApproverEntity uatApprover2 = buildUatApprover(uatId, uatOnlyUserId, true, 2);

        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(requestId))
                .thenReturn(List.of(crApprover1, crApprover2));
        when(uatApproverRepository.findByUatIdOrderByPositionAsc(uatId))
                .thenReturn(List.of(uatApprover1, uatApprover2));

        deploymentService.createFromPromotion(requestId, uatId, actorId);

        ArgumentCaptor<RequestDeploymentApproverEntity> captor =
                ArgumentCaptor.forClass(RequestDeploymentApproverEntity.class);
        verify(deploymentApproverRepository, org.mockito.Mockito.times(3)).save(captor.capture());

        List<RequestDeploymentApproverEntity> saved = captor.getAllValues();
        assertThat(saved).hasSize(3);

        List<UUID> savedUserIds = saved.stream()
                .map(RequestDeploymentApproverEntity::getUserId)
                .toList();
        assertThat(savedUserIds).containsExactlyInAnyOrder(sharedUserId, crOnlyUserId, uatOnlyUserId);
    }

    @Test
    void createFromPromotionPreservesRequiredFlagFromCrWhenRequired() {
        UUID requestId = UUID.randomUUID();
        UUID uatId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID sharedUserId = UUID.randomUUID();

        when(deploymentRepository.existsByRequestId(requestId)).thenReturn(false);
        when(deploymentRepository.save(any(RequestDeploymentEntity.class))).thenAnswer(inv -> {
            RequestDeploymentEntity d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });

        UserEntity sharedUser = buildUser(sharedUserId);
        CrApproverEntity crApprover = buildCrApprover(requestId, sharedUser, true, 1);
        RequestUatApproverEntity uatApprover = buildUatApprover(uatId, sharedUserId, false, 1);

        when(crApproverRepository.findByChangeRequestIdOrderByPositionAsc(requestId))
                .thenReturn(List.of(crApprover));
        when(uatApproverRepository.findByUatIdOrderByPositionAsc(uatId))
                .thenReturn(List.of(uatApprover));

        deploymentService.createFromPromotion(requestId, uatId, actorId);

        ArgumentCaptor<RequestDeploymentApproverEntity> captor =
                ArgumentCaptor.forClass(RequestDeploymentApproverEntity.class);
        verify(deploymentApproverRepository).save(captor.capture());

        RequestDeploymentApproverEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(sharedUserId);
        assertThat(saved.isRequired()).isTrue();
    }

    @Test
    void approveDeploymentSetsStatusApprovedWhenAllRequiredApproved() {
        UUID deploymentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        RequestDeploymentEntity deployment = buildDeployment(deploymentId, UUID.randomUUID(), UUID.randomUUID());

        RequestDeploymentApproverEntity approver1 = buildDeploymentApprover(deploymentId, actorId, true, 1);
        RequestDeploymentApproverEntity approver2 = buildDeploymentApprover(deploymentId, UUID.randomUUID(), true, 2);
        approver2.setStatus(ApproverStatus.APPROVED);

        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
        when(deploymentApproverRepository.findByDeploymentIdAndUserId(deploymentId, actorId))
                .thenReturn(Optional.of(approver1));
        when(deploymentApproverRepository.findByDeploymentIdOrderByPositionAsc(deploymentId))
                .thenReturn(List.of(approver1, approver2));
        when(deploymentRepository.save(any(RequestDeploymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        deploymentService.approveDeployment(deploymentId, actorId);

        assertThat(approver1.getStatus()).isEqualTo(ApproverStatus.APPROVED);
        assertThat(deployment.getStatus()).isEqualTo("APPROVED");
        assertThat(deployment.getCompletedAt()).isNotNull();
        verify(deploymentRepository).save(deployment);
    }

    @Test
    void approveDeploymentDoesNotSetApprovedWhenRequiredStillPending() {
        UUID deploymentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        RequestDeploymentEntity deployment = buildDeployment(deploymentId, UUID.randomUUID(), UUID.randomUUID());

        RequestDeploymentApproverEntity approver1 = buildDeploymentApprover(deploymentId, actorId, true, 1);
        RequestDeploymentApproverEntity approver2 = buildDeploymentApprover(deploymentId, UUID.randomUUID(), true, 2);

        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
        when(deploymentApproverRepository.findByDeploymentIdAndUserId(deploymentId, actorId))
                .thenReturn(Optional.of(approver1));
        when(deploymentApproverRepository.findByDeploymentIdOrderByPositionAsc(deploymentId))
                .thenReturn(List.of(approver1, approver2));

        deploymentService.approveDeployment(deploymentId, actorId);

        assertThat(approver1.getStatus()).isEqualTo(ApproverStatus.APPROVED);
        assertThat(deployment.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(deployment.getCompletedAt()).isNull();
        verify(deploymentRepository, never()).save(any());
    }

    @Test
    void rejectDeploymentSetsStatusRejected() {
        UUID deploymentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        RequestDeploymentEntity deployment = buildDeployment(deploymentId, UUID.randomUUID(), UUID.randomUUID());
        RequestDeploymentApproverEntity approver = buildDeploymentApprover(deploymentId, actorId, true, 1);

        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
        when(deploymentApproverRepository.findByDeploymentIdAndUserId(deploymentId, actorId))
                .thenReturn(Optional.of(approver));
        when(deploymentRepository.save(any(RequestDeploymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        deploymentService.rejectDeployment(deploymentId, actorId, "Not ready");

        assertThat(approver.getStatus()).isEqualTo(ApproverStatus.REJECTED);
        assertThat(approver.getRejectionReason()).isEqualTo("Not ready");
        assertThat(deployment.getStatus()).isEqualTo("REJECTED");
        verify(deploymentRepository).save(deployment);
    }

    @Test
    void isDeploymentDoneReturnsTrueWhenApproved() {
        UUID requestId = UUID.randomUUID();
        RequestDeploymentEntity deployment = buildDeployment(UUID.randomUUID(), requestId, UUID.randomUUID());
        deployment.setStatus("APPROVED");

        when(deploymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(deployment));

        assertThat(deploymentService.isDeploymentDone(requestId)).isTrue();
    }

    @Test
    void isDeploymentDoneReturnsFalseWhenPending() {
        UUID requestId = UUID.randomUUID();
        RequestDeploymentEntity deployment = buildDeployment(UUID.randomUUID(), requestId, UUID.randomUUID());
        deployment.setStatus("PENDING_APPROVAL");

        when(deploymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(deployment));

        assertThat(deploymentService.isDeploymentDone(requestId)).isFalse();
    }

    @Test
    void isDeploymentDoneReturnsFalseWhenNoDeployment() {
        UUID requestId = UUID.randomUUID();
        when(deploymentRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        assertThat(deploymentService.isDeploymentDone(requestId)).isFalse();
    }

    private UserEntity buildUser(UUID userId) {
        UserEntity user = new UserEntity("user@example.com", "Test User");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private CrApproverEntity buildCrApprover(UUID requestId, UserEntity user, boolean required, int position) {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        CrApproverEntity approver = new CrApproverEntity(cr, user, required, position, false);
        ReflectionTestUtils.setField(approver, "id", UUID.randomUUID());
        return approver;
    }

    private RequestUatApproverEntity buildUatApprover(UUID uatId, UUID userId, boolean required, int position) {
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        ReflectionTestUtils.setField(approver, "id", UUID.randomUUID());
        approver.setUatId(uatId);
        approver.setUserId(userId);
        approver.setRequired(required);
        approver.setPosition(position);
        return approver;
    }

    private RequestDeploymentEntity buildDeployment(UUID deploymentId, UUID requestId, UUID uatId) {
        RequestDeploymentEntity deployment = new RequestDeploymentEntity();
        ReflectionTestUtils.setField(deployment, "id", deploymentId);
        deployment.setRequestId(requestId);
        deployment.setUatId(uatId);
        deployment.setStatus("PENDING_APPROVAL");
        return deployment;
    }

    private RequestDeploymentApproverEntity buildDeploymentApprover(UUID deploymentId, UUID userId,
            boolean required, int position) {
        RequestDeploymentApproverEntity approver = new RequestDeploymentApproverEntity();
        ReflectionTestUtils.setField(approver, "id", UUID.randomUUID());
        approver.setDeploymentId(deploymentId);
        approver.setUserId(userId);
        approver.setRequired(required);
        approver.setPosition(position);
        approver.setStatus(ApproverStatus.PENDING);
        return approver;
    }
}
