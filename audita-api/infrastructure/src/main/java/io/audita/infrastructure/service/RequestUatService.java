package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestUatApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestUatCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestUatService {

    private static final Set<String> ELEVATED_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final String ENTITY_UAT = "request_uat";

    private final ChangeRequestRepository changeRequestRepository;
    private final RequestUatRepository requestUatRepository;
    private final RequestUatApproverRepository requestUatApproverRepository;
    private final RequestUatCommentRepository requestUatCommentRepository;
    private final UserRepository userRepository;
    private final RequestDeploymentService deploymentService;
    private final AuditLogService auditLogService;
    private final ActivityStreamRepository activityStreamRepository;

    public RequestUatService(ChangeRequestRepository changeRequestRepository,
            RequestUatRepository requestUatRepository,
            RequestUatApproverRepository requestUatApproverRepository,
            RequestUatCommentRepository requestUatCommentRepository,
            UserRepository userRepository,
            RequestDeploymentService deploymentService,
            AuditLogService auditLogService,
            ActivityStreamRepository activityStreamRepository) {
        this.changeRequestRepository = changeRequestRepository;
        this.requestUatRepository = requestUatRepository;
        this.requestUatApproverRepository = requestUatApproverRepository;
        this.requestUatCommentRepository = requestUatCommentRepository;
        this.userRepository = userRepository;
        this.deploymentService = deploymentService;
        this.auditLogService = auditLogService;
        this.activityStreamRepository = activityStreamRepository;
    }

    public RequestUatEntity createUat(UUID requestId, String title, String details,
            UUID actorUserId, String actorRole) {
        ChangeRequestEntity cr = loadRequest(requestId);
        assertCanMutate(cr, actorUserId, actorRole);

        if (cr.getApprovalStatus() != ChangeRequestStatus.APPROVED) {
            throw new InvalidStateTransitionException(
                    "UAT can only be created after the request is approved.");
        }
        if (cr.getWorkflowMode() != RequestWorkflowMode.DELIVERY_PIPELINE) {
            throw new InvalidStateTransitionException(
                    "UAT is only available for DELIVERY_PIPELINE mode requests.");
        }
        if (requestUatRepository.existsByRequestId(requestId)) {
            throw new InvalidStateTransitionException(
                    "A UAT record already exists for this request.");
        }

        RequestUatEntity uat = new RequestUatEntity();
        uat.setRequestId(requestId);
        uat.setTitle(title);
        uat.setDetails(details);
        uat.setCreatedBy(actorUserId);

        RequestUatEntity saved = requestUatRepository.save(uat);
        auditLogService.log("UAT_CREATED", ENTITY_UAT, saved.getId(),
                actorUserId, null,
                Map.of("requestId", requestId.toString(), "title", title), null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_CREATED",
                Map.of("requestId", requestId.toString(), "title", title));
        return saved;
    }

    public RequestUatEntity updateUat(UUID uatId, String title, String details,
            UUID actorUserId, String actorRole) {
        RequestUatEntity uat = loadUat(uatId);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException(
                    "UAT is read-only after promotion and cannot be edited.");
        }

        if (title != null) {
            uat.setTitle(title);
        }
        if (details != null) {
            uat.setDetails(details);
        }

        RequestUatEntity saved = requestUatRepository.save(uat);
        auditLogService.log("UAT_UPDATED", ENTITY_UAT, saved.getId(),
                actorUserId, null,
                Map.of("requestId", uat.getRequestId().toString()), null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_UPDATED",
                Map.of("requestId", uat.getRequestId().toString()));
        return saved;
    }

    public RequestUatApproverEntity addApprover(UUID uatId, UUID userId, boolean isRequired,
            UUID actorUserId, String actorRole) {
        RequestUatEntity uat = loadUat(uatId);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException(
                    "Cannot add approvers to a read-only UAT.");
        }

        if (requestUatApproverRepository.findByUatIdAndUserId(uatId, userId).isPresent()) {
            throw new DomainNotPermittedException("DUPLICATE_APPROVER",
                    "User is already an approver on this UAT.");
        }

        int position = requestUatApproverRepository.countByUatId(uatId) + 1;
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        approver.setUatId(uatId);
        approver.setUserId(userId);
        approver.setRequired(isRequired);
        approver.setPosition(position);

        RequestUatApproverEntity saved = requestUatApproverRepository.save(approver);
        auditLogService.log("UAT_APPROVER_ADDED", ENTITY_UAT, uatId,
                actorUserId, null,
                Map.of("approverUserId", userId.toString(), "isRequired", isRequired), null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_APPROVER_ADDED",
                Map.of("approverUserId", userId.toString(), "isRequired", isRequired));
        return saved;
    }

    public void approveUat(UUID uatId, UUID actorUserId) {
        RequestUatEntity uat = loadUat(uatId);
        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted; approvals are locked.");
        }
        RequestUatApproverEntity approver = loadApprover(uatId, actorUserId);
        approver.approve();
        requestUatApproverRepository.save(approver);
        auditLogService.log("UAT_APPROVED", ENTITY_UAT, uatId,
                actorUserId, null, Map.of(), null);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_APPROVED", Map.of());
    }

    public void rejectUat(UUID uatId, UUID actorUserId, String reason) {
        RequestUatEntity uat = loadUat(uatId);
        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted; approvals are locked.");
        }
        RequestUatApproverEntity approver = loadApprover(uatId, actorUserId);
        approver.reject(reason);
        requestUatApproverRepository.save(approver);
        auditLogService.log("UAT_REJECTED", ENTITY_UAT, uatId,
                actorUserId, null, Map.of("reason", reason), null);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_REJECTED", Map.of("reason", reason));
    }

    public RequestUatEntity promoteToDeployment(UUID uatId, UUID actorUserId, String actorRole) {
        RequestUatEntity uat = loadUat(uatId);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted.");
        }

        List<RequestUatApproverEntity> approvers =
                requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId);

        List<RequestUatApproverEntity> required = approvers.stream()
                .filter(RequestUatApproverEntity::isRequired)
                .toList();

        if (required.isEmpty()) {
            throw new InvalidStateTransitionException(
                    "At least one required approver must be assigned before promotion.");
        }

        boolean allRequiredApproved = required.stream()
                .allMatch(a -> a.getStatus() == ApproverStatus.APPROVED);

        if (!allRequiredApproved) {
            throw new InvalidStateTransitionException(
                    "All required UAT approvers must approve before promotion.");
        }

        uat.setReadOnly(true);
        uat.setStatus("PROMOTED");

        RequestUatEntity saved = requestUatRepository.save(uat);

        deploymentService.createFromPromotion(uat.getRequestId(), uatId, actorUserId);

        auditLogService.log("UAT_PROMOTED", ENTITY_UAT, saved.getId(),
                actorUserId, null,
                Map.of("requestId", uat.getRequestId().toString()), null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_PROMOTED",
                Map.of("requestId", uat.getRequestId().toString()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<RequestUatEntity> getByRequestId(UUID requestId) {
        return requestUatRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public List<RequestUatApproverEntity> listApprovers(UUID uatId) {
        return requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId);
    }

    @Transactional(readOnly = true)
    public List<RequestUatCommentEntity> listComments(UUID uatId) {
        return requestUatCommentRepository.findByUatIdOrderByCreatedAtDesc(uatId);
    }

    public RequestUatCommentEntity createComment(UUID uatId, UUID authorId, String body) {
        if (body == null || body.isBlank()) {
            throw new DomainNotPermittedException("INVALID_INPUT", "Comment body is required.");
        }
        RequestUatEntity uat = loadUat(uatId);
        RequestUatCommentEntity saved = requestUatCommentRepository.save(new RequestUatCommentEntity(uatId, authorId, body));
        auditLogService.log("UAT_COMMENT_ADDED", ENTITY_UAT, uatId,
                authorId, null,
                Map.of("requestId", uat.getRequestId().toString(), "commentId", saved.getId().toString()),
                null);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        UserEntity actor = userRepository.findById(authorId).orElse(null);
        logActivity(cr, actor, "UAT_COMMENT_ADDED", Map.of("commentId", saved.getId().toString()));
        return saved;
    }

    public Map<UUID, UserEntity> loadAuthors(List<RequestUatCommentEntity> comments) {
        List<UUID> authorIds = comments.stream()
                .map(RequestUatCommentEntity::getAuthorId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    private ChangeRequestEntity loadRequest(UUID requestId) {
        return changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Request not found."));
    }

    private RequestUatEntity loadUat(UUID uatId) {
        return requestUatRepository.findById(uatId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "UAT record not found."));
    }

    private RequestUatApproverEntity loadApprover(UUID uatId, UUID userId) {
        return requestUatApproverRepository.findByUatIdAndUserId(uatId, userId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER",
                        "You are not an approver on this UAT."));
    }

    private void assertCanMutate(ChangeRequestEntity cr, UUID actorUserId, String actorRole) {
        String normalizedRole = actorRole == null ? "" : actorRole.toUpperCase();
        if (ELEVATED_ROLES.contains(normalizedRole)) {
            return;
        }
        if (cr.getCreatedBy() != null && cr.getCreatedBy().getId().equals(actorUserId)) {
            return;
        }
        throw new DomainNotPermittedException("FORBIDDEN",
                "You are not allowed to modify this UAT record.");
    }

    private void logActivity(ChangeRequestEntity changeRequest, UserEntity actor,
            String actionType, Map<String, Object> payload) {
        activityStreamRepository.save(new ActivityStreamEntity(changeRequest, actor, actionType, payload));
    }
}
