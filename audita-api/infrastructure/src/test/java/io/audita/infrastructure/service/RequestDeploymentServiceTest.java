package io.audita.infrastructure.service;

import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestDeploymentServiceTest {

    @Mock
    RequestDeploymentRepository deploymentRepository;
    @Mock
    RequestDeploymentCommentRepository deploymentCommentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    AuditLogService auditLogService;
    @Mock
    ActivityStreamRepository activityStreamRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    MentionNotifier mentionNotifier;

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

        RequestDeploymentEntity result = deploymentService.createFromPromotion(requestId, uatId, actorId);

        assertThat(result.getRequestId()).isEqualTo(requestId);
        assertThat(result.getUatId()).isEqualTo(uatId);
        assertThat(result.getCreatedBy()).isEqualTo(actorId);
        assertThat(result.getStatus()).isEqualTo("PENDING_APPROVAL");
        verify(deploymentRepository).save(any(RequestDeploymentEntity.class));
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

    private RequestDeploymentEntity buildDeployment(UUID deploymentId, UUID requestId, UUID uatId) {
        RequestDeploymentEntity deployment = new RequestDeploymentEntity();
        ReflectionTestUtils.setField(deployment, "id", deploymentId);
        deployment.setRequestId(requestId);
        deployment.setUatId(uatId);
        deployment.setStatus("PENDING_APPROVAL");
        return deployment;
    }
}
