package io.audita.infrastructure.service;

import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestUatCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatRepository;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestWorkflowAuditTest {

    // ── UAT audit tests ──────────────────────────────────────────────────────

    @Nested
    class UatAuditTests {

        @Mock ChangeRequestRepository changeRequestRepository;
        @Mock RequestUatRepository requestUatRepository;
        @Mock RequestUatApproverRepository requestUatApproverRepository;
        @Mock RequestUatCommentRepository requestUatCommentRepository;
        @Mock UserRepository userRepository;
        @Mock RequestDeploymentService deploymentService;
        @Mock AuditLogService auditLogService;
        @Mock ActivityStreamRepository activityStreamRepository;

        @InjectMocks RequestUatService requestUatService;

        @Test
        void createCommentLogsUatCommentAdded() {
            UUID uatId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            RequestUatEntity uat = new RequestUatEntity();
            ReflectionTestUtils.setField(uat, "id", uatId);
            uat.setRequestId(requestId);

            ChangeRequestEntity cr = new ChangeRequestEntity();
            ReflectionTestUtils.setField(cr, "id", requestId);

            when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
            when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
            when(userRepository.findById(authorId)).thenReturn(Optional.empty());
            when(requestUatCommentRepository.save(any(RequestUatCommentEntity.class))).thenAnswer(inv -> {
                RequestUatCommentEntity c = inv.getArgument(0);
                ReflectionTestUtils.setField(c, "id", commentId);
                return c;
            });

            requestUatService.createComment(uatId, authorId, "looks good");

            verify(auditLogService).log(
                    eq("UAT_COMMENT_ADDED"),
                    eq("request_uat"),
                    eq(uatId),
                    eq(authorId),
                    any(),
                    any(),
                    any());
        }

        @Test
        void createCommentPayloadIncludesRequestId() {
            UUID uatId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            RequestUatEntity uat = new RequestUatEntity();
            ReflectionTestUtils.setField(uat, "id", uatId);
            uat.setRequestId(requestId);

            ChangeRequestEntity cr = new ChangeRequestEntity();
            ReflectionTestUtils.setField(cr, "id", requestId);

            when(requestUatRepository.findById(uatId)).thenReturn(Optional.of(uat));
            when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
            when(userRepository.findById(authorId)).thenReturn(Optional.empty());
            when(requestUatCommentRepository.save(any(RequestUatCommentEntity.class))).thenAnswer(inv -> {
                RequestUatCommentEntity c = inv.getArgument(0);
                ReflectionTestUtils.setField(c, "id", commentId);
                return c;
            });

            requestUatService.createComment(uatId, authorId, "test comment");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(auditLogService).log(
                    eq("UAT_COMMENT_ADDED"),
                    eq("request_uat"),
                    eq(uatId),
                    eq(authorId),
                    any(),
                    payloadCaptor.capture(),
                    any());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsEntry("requestId", requestId.toString());
            assertThat(payload).containsEntry("commentId", commentId.toString());
            assertThat(payload).doesNotContainKey("password");
            assertThat(payload).doesNotContainKey("token");
        }

        @Test
        void createUatLogsUatCreated() {
            UUID requestId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();
            ChangeRequestEntity cr = buildApprovedDeliveryPipelineRequest(requestId);

            when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
            when(requestUatRepository.existsByRequestId(requestId)).thenReturn(false);
            when(requestUatRepository.save(any(RequestUatEntity.class))).thenAnswer(inv -> {
                RequestUatEntity u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
                return u;
            });

            requestUatService.createUat(requestId, "title", "details", actorId, "ADMIN");

            verify(auditLogService).log(
                    eq("UAT_CREATED"),
                    eq("request_uat"),
                    any(),
                    eq(actorId),
                    any(),
                    any(),
                    any());
        }
    }

    // ── Deployment audit tests ───────────────────────────────────────────────

    @Nested
    class DeploymentAuditTests {

        @Mock RequestDeploymentRepository deploymentRepository;
        @Mock RequestDeploymentApproverRepository deploymentApproverRepository;
        @Mock RequestDeploymentCommentRepository deploymentCommentRepository;
        @Mock CrApproverRepository crApproverRepository;
        @Mock RequestUatApproverRepository uatApproverRepository;
        @Mock UserRepository userRepository;
        @Mock ChangeRequestRepository changeRequestRepository;
        @Mock ActivityStreamRepository activityStreamRepository;
        @Mock AuditLogService auditLogService;

        @InjectMocks RequestDeploymentService requestDeploymentService;

        @Test
        void createCommentLogsDeploymentCommentAdded() {
            UUID deploymentId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            RequestDeploymentEntity deployment = new RequestDeploymentEntity();
            ReflectionTestUtils.setField(deployment, "id", deploymentId);
            deployment.setRequestId(requestId);

            when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
            when(deploymentCommentRepository.save(any(RequestDeploymentCommentEntity.class))).thenAnswer(inv -> {
                RequestDeploymentCommentEntity c = inv.getArgument(0);
                ReflectionTestUtils.setField(c, "id", commentId);
                return c;
            });

            requestDeploymentService.createComment(deploymentId, authorId, "approved in prod");

            verify(auditLogService).log(
                    eq("DEPLOYMENT_COMMENT_ADDED"),
                    eq("request_deployment"),
                    eq(deploymentId),
                    eq(authorId),
                    any(),
                    any(),
                    any());
        }

        @Test
        void createCommentPayloadIncludesRequestIdAndNoSensitiveData() {
            UUID deploymentId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            RequestDeploymentEntity deployment = new RequestDeploymentEntity();
            ReflectionTestUtils.setField(deployment, "id", deploymentId);
            deployment.setRequestId(requestId);

            when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
            when(deploymentCommentRepository.save(any(RequestDeploymentCommentEntity.class))).thenAnswer(inv -> {
                RequestDeploymentCommentEntity c = inv.getArgument(0);
                ReflectionTestUtils.setField(c, "id", commentId);
                return c;
            });

            requestDeploymentService.createComment(deploymentId, authorId, "comment body");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(auditLogService).log(
                    eq("DEPLOYMENT_COMMENT_ADDED"),
                    eq("request_deployment"),
                    eq(deploymentId),
                    eq(authorId),
                    any(),
                    payloadCaptor.capture(),
                    any());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsEntry("requestId", requestId.toString());
            assertThat(payload).containsEntry("commentId", commentId.toString());
            assertThat(payload).doesNotContainKey("password");
            assertThat(payload).doesNotContainKey("token");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static ChangeRequestEntity buildApprovedDeliveryPipelineRequest(UUID requestId) {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setApprovalStatus(ChangeRequestStatus.APPROVED);
        cr.setWorkflowMode(RequestWorkflowMode.DELIVERY_PIPELINE);
        return cr;
    }
}
