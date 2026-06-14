package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApproverStatus;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentCommentEntity;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestDeploymentService {

    private static final String ENTITY_DEPLOYMENT = "request_deployment";

    private final RequestDeploymentRepository deploymentRepository;
    private final RequestDeploymentApproverRepository deploymentApproverRepository;
    private final RequestDeploymentCommentRepository deploymentCommentRepository;
    private final CrApproverRepository crApproverRepository;
    private final RequestUatApproverRepository uatApproverRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ActivityStreamRepository activityStreamRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final MentionNotifier mentionNotifier;

    public RequestDeploymentService(RequestDeploymentRepository deploymentRepository,
            RequestDeploymentApproverRepository deploymentApproverRepository,
            RequestDeploymentCommentRepository deploymentCommentRepository,
            CrApproverRepository crApproverRepository,
            RequestUatApproverRepository uatApproverRepository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            ActivityStreamRepository activityStreamRepository,
            ChangeRequestRepository changeRequestRepository,
            NotificationService notificationService,
            EmailService emailService,
            MentionNotifier mentionNotifier) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentApproverRepository = deploymentApproverRepository;
        this.deploymentCommentRepository = deploymentCommentRepository;
        this.crApproverRepository = crApproverRepository;
        this.uatApproverRepository = uatApproverRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.activityStreamRepository = activityStreamRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.mentionNotifier = mentionNotifier;
    }

    public RequestDeploymentEntity createFromPromotion(UUID requestId, UUID uatId, UUID actorUserId) {
        if (deploymentRepository.existsByRequestId(requestId)) {
            throw new InvalidStateTransitionException(
                    "A deployment already exists for this request.");
        }

        RequestDeploymentEntity deployment = new RequestDeploymentEntity();
        deployment.setRequestId(requestId);
        deployment.setUatId(uatId);
        deployment.setCreatedBy(actorUserId);
        deployment.setPromotedAt(OffsetDateTime.now());

        RequestDeploymentEntity saved = deploymentRepository.save(deployment);

        Map<UUID, ApproverSource> mergedApprovers = new LinkedHashMap<>();

        List<CrApproverEntity> crApprovers = crApproverRepository
                .findByChangeRequestIdOrderByPositionAsc(requestId);
        for (CrApproverEntity cr : crApprovers) {
            UUID userId = cr.getUser().getId();
            mergedApprovers.putIfAbsent(userId, new ApproverSource(userId, cr.isRequired()));
        }

        List<RequestUatApproverEntity> uatApprovers = uatApproverRepository
                .findByUatIdOrderByPositionAsc(uatId);
        for (RequestUatApproverEntity uat : uatApprovers) {
            UUID userId = uat.getUserId();
            ApproverSource existing = mergedApprovers.get(userId);
            if (existing == null) {
                mergedApprovers.put(userId, new ApproverSource(userId, uat.isRequired()));
            } else if (uat.isRequired()) {
                existing.required = true;
            }
        }

        int position = 1;
        for (ApproverSource src : mergedApprovers.values()) {
            RequestDeploymentApproverEntity approver = new RequestDeploymentApproverEntity();
            approver.setDeploymentId(saved.getId());
            approver.setUserId(src.userId);
            approver.setRequired(src.required);
            approver.setPosition(position++);
            deploymentApproverRepository.save(approver);
        }

        auditLogService.log("DEPLOYMENT_CREATED", ENTITY_DEPLOYMENT, saved.getId(),
                actorUserId, null,
                Map.of("requestId", requestId.toString(), "uatId", uatId.toString()), null);

        ChangeRequestEntity cr = changeRequestRepository.findById(requestId).orElse(null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "DEPLOYMENT_CREATED",
                Map.of("requestId", requestId.toString(), "uatId", uatId.toString()));

        for (ApproverSource src : mergedApprovers.values()) {
            notificationService.createAndPush(src.userId, "DEPLOYMENT_APPROVAL_REQUESTED",
                    "Deployment review needed: " + (cr != null ? cr.getTitle() : "Change Request"),
                    "Your review is needed for this deployment.",
                    "/change-requests/" + requestId);
            UserEntity approverUser = userRepository.findById(src.userId).orElse(null);
            if (approverUser != null) {
                emailService.sendDeploymentApprovalRequestEmail(approverUser.getEmail(),
                        approverUser.getFullName(),
                        cr != null ? cr.getTitle() : "Change Request",
                        saved.getId().toString());
            }
        }

        return saved;
    }

    public void approveDeployment(UUID deploymentId, UUID actorUserId) {
        RequestDeploymentEntity deployment = loadDeployment(deploymentId);
        if (!"PENDING_APPROVAL".equals(deployment.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Deployment is no longer open for approval. Current status: " + deployment.getStatus());
        }
        RequestDeploymentApproverEntity approver = loadApprover(deploymentId, actorUserId);

        approver.approve();
        deploymentApproverRepository.save(approver);

        List<RequestDeploymentApproverEntity> allApprovers =
                deploymentApproverRepository.findByDeploymentIdOrderByPositionAsc(deploymentId);

        boolean allRequiredApproved = allApprovers.stream()
                .filter(RequestDeploymentApproverEntity::isRequired)
                .allMatch(a -> a.getStatus() == ApproverStatus.APPROVED);

        if (allRequiredApproved) {
            deployment.setStatus("APPROVED");
            deployment.setCompletedAt(OffsetDateTime.now());
            deploymentRepository.save(deployment);
        }

        auditLogService.log("DEPLOYMENT_APPROVED", ENTITY_DEPLOYMENT, deploymentId,
                actorUserId, null, Map.of(), null);

        ChangeRequestEntity cr = changeRequestRepository.findById(deployment.getRequestId()).orElse(null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "DEPLOYMENT_APPROVED", Map.of());
        if (cr != null && cr.getCreatedBy() != null) {
            notificationService.createAndPush(cr.getCreatedBy().getId(), "DEPLOYMENT_APPROVAL_DECIDED",
                    actor.getFullName() + " approved deployment: " + cr.getTitle(),
                    "A deployment approver has approved.",
                    "/change-requests/" + cr.getId());
            emailService.sendDeploymentApprovalDecisionEmail(cr.getCreatedBy().getEmail(),
                    cr.getCreatedBy().getFullName(), cr.getTitle(),
                    deploymentId.toString(), "approved",
                    actor != null ? actor.getFullName() : "Someone");
        }
    }

    public void rejectDeployment(UUID deploymentId, UUID actorUserId, String reason) {
        RequestDeploymentEntity deployment = loadDeployment(deploymentId);
        if (!"PENDING_APPROVAL".equals(deployment.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Deployment is no longer open for rejection. Current status: " + deployment.getStatus());
        }
        RequestDeploymentApproverEntity approver = loadApprover(deploymentId, actorUserId);

        approver.reject(reason);
        deploymentApproverRepository.save(approver);

        deployment.setStatus("REJECTED");
        deploymentRepository.save(deployment);

        auditLogService.log("DEPLOYMENT_REJECTED", ENTITY_DEPLOYMENT, deploymentId,
                actorUserId, null, Map.of("reason", reason), null);

        ChangeRequestEntity cr = changeRequestRepository.findById(deployment.getRequestId()).orElse(null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "DEPLOYMENT_REJECTED", Map.of("reason", reason));
        if (cr != null && cr.getCreatedBy() != null) {
            notificationService.createAndPush(cr.getCreatedBy().getId(), "DEPLOYMENT_APPROVAL_DECIDED",
                    actor.getFullName() + " rejected deployment: " + cr.getTitle(),
                    "A deployment approver has rejected. Reason: " + (reason != null ? reason : "No reason provided"),
                    "/change-requests/" + cr.getId());
            emailService.sendDeploymentApprovalDecisionEmail(cr.getCreatedBy().getEmail(),
                    cr.getCreatedBy().getFullName(), cr.getTitle(),
                    deploymentId.toString(), "rejected",
                    actor != null ? actor.getFullName() : "Someone");
        }
    }

    @Transactional(readOnly = true)
    public Optional<RequestDeploymentEntity> getByRequestId(UUID requestId) {
        return deploymentRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public List<RequestDeploymentApproverEntity> listApprovers(UUID deploymentId) {
        return deploymentApproverRepository.findByDeploymentIdOrderByPositionAsc(deploymentId);
    }

    @Transactional(readOnly = true)
    public List<RequestDeploymentCommentEntity> listComments(UUID deploymentId) {
        return deploymentCommentRepository.findByDeploymentIdOrderByCreatedAtDesc(deploymentId);
    }

    public RequestDeploymentCommentEntity createComment(UUID deploymentId, UUID authorId, String body) {
        if (body == null || body.isBlank()) {
            throw new DomainNotPermittedException("INVALID_INPUT", "Comment body is required.");
        }
        RequestDeploymentEntity deployment = loadDeployment(deploymentId);
        RequestDeploymentCommentEntity saved = deploymentCommentRepository.save(new RequestDeploymentCommentEntity(deploymentId, authorId, body));
        auditLogService.log("DEPLOYMENT_COMMENT_ADDED", ENTITY_DEPLOYMENT, deploymentId,
                authorId, null,
                Map.of("requestId", deployment.getRequestId().toString(), "commentId", saved.getId().toString()),
                null);
        ChangeRequestEntity cr = changeRequestRepository.findById(deployment.getRequestId()).orElse(null);
        UserEntity actor = userRepository.findById(authorId).orElse(null);
        logActivity(cr, actor, "DEPLOYMENT_COMMENT_ADDED", Map.of("commentId", saved.getId().toString()));
        mentionNotifier.processMentions(body, authorId, cr.getTitle(), "/change-requests/" + deployment.getRequestId());
        return saved;
    }

    public Map<UUID, UserEntity> loadAuthors(List<RequestDeploymentCommentEntity> comments) {
        List<UUID> authorIds = comments.stream()
                .map(RequestDeploymentCommentEntity::getAuthorId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    @Transactional(readOnly = true)
    public boolean isDeploymentDone(UUID requestId) {
        return deploymentRepository.findByRequestId(requestId)
                .map(d -> "APPROVED".equals(d.getStatus()))
                .orElse(false);
    }

    private RequestDeploymentEntity loadDeployment(UUID deploymentId) {
        return deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Deployment not found."));
    }

    private RequestDeploymentApproverEntity loadApprover(UUID deploymentId, UUID userId) {
        return deploymentApproverRepository.findByDeploymentIdAndUserId(deploymentId, userId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER",
                        "You are not an approver on this deployment."));
    }

    private void logActivity(ChangeRequestEntity changeRequest, UserEntity actor,
            String actionType, Map<String, Object> payload) {
        activityStreamRepository.save(new ActivityStreamEntity(changeRequest, actor, actionType, payload));
    }

    private static class ApproverSource {
        final UUID userId;
        boolean required;

        ApproverSource(UUID userId, boolean required) {
            this.userId = userId;
            this.required = required;
        }
    }
}
