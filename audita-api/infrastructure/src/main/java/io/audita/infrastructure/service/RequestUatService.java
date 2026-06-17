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
import io.audita.infrastructure.tenant.RequestContext;
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
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final MentionNotifier mentionNotifier;

    public RequestUatService(ChangeRequestRepository changeRequestRepository,
            RequestUatRepository requestUatRepository,
            RequestUatApproverRepository requestUatApproverRepository,
            RequestUatCommentRepository requestUatCommentRepository,
            UserRepository userRepository,
            RequestDeploymentService deploymentService,
            AuditLogService auditLogService,
            ActivityStreamRepository activityStreamRepository,
            NotificationService notificationService,
            EmailService emailService,
            MentionNotifier mentionNotifier) {
        this.changeRequestRepository = changeRequestRepository;
        this.requestUatRepository = requestUatRepository;
        this.requestUatApproverRepository = requestUatApproverRepository;
        this.requestUatCommentRepository = requestUatCommentRepository;
        this.userRepository = userRepository;
        this.deploymentService = deploymentService;
        this.auditLogService = auditLogService;
        this.activityStreamRepository = activityStreamRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.mentionNotifier = mentionNotifier;
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
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("requestId", requestId.toString(), "title", title), RequestContext.getCurrentIp());
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
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("requestId", uat.getRequestId().toString()), RequestContext.getCurrentIp());
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
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("approverUserId", userId.toString(), "isRequired", isRequired), RequestContext.getCurrentIp());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_APPROVER_ADDED",
                Map.of("approverUserId", userId.toString(), "isRequired", isRequired));
        notificationService.createAndPush(userId, "UAT_APPROVER_ADDED",
                "Added as UAT approver: " + cr.getTitle(),
                "You have been added as a UAT approver.",
                "/change-requests/" + cr.getId());
        UserEntity addedUser = userRepository.findById(userId).orElse(null);
        if (addedUser != null) {
            emailService.sendUatApprovalRequestEmail(addedUser.getEmail(), addedUser.getFullName(),
                    cr.getTitle(), uat.getId().toString());
        }
        return saved;
    }

    public RequestUatApproverEntity updateApproverRequirement(UUID requestId,
            UUID approverId, boolean isRequired, UUID actorUserId, String actorRole) {
        RequestUatEntity uat = getByRequestId(requestId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "UAT not found"));
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("Cannot update approvers on a read-only UAT.");
        }

        RequestUatApproverEntity approver = requestUatApproverRepository.findById(approverId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Approver not found."));
        if (!approver.getUatId().equals(uat.getId())) {
            throw new DomainNotPermittedException("NOT_FOUND", "Approver not found on this UAT.");
        }

        approver.setRequired(isRequired);
        requestUatApproverRepository.save(approver);
        auditLogService.log("UAT_APPROVER_REQUIREMENT_CHANGED", ENTITY_UAT, uat.getId(),
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("approverId", approverId.toString(), "isRequired", isRequired),
                RequestContext.getCurrentIp());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_APPROVER_REQUIREMENT_CHANGED",
                Map.of("approverId", approverId.toString(), "isRequired", isRequired));
        return approver;
    }

    public void approveUat(UUID uatId, UUID actorUserId) {
        RequestUatEntity uat = loadUat(uatId);
        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted; approvals are locked.");
        }
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());

        // CR requester can sign-off without being a listed approver
        if (cr.getCreatedBy() != null && cr.getCreatedBy().getId().equals(actorUserId)) {
            uat.setRequesterSignedOff(true);
            requestUatRepository.save(uat);
            auditLogService.log("UAT_REQUESTER_SIGNED_OFF", ENTITY_UAT, uat.getId(),
                    actorUserId, resolveActorEmail(actorUserId),
                    Map.of("requestId", cr.getId().toString()),
                    RequestContext.getCurrentIp());
            UserEntity actor = userRepository.findById(actorUserId).orElse(null);
            logActivity(cr, actor, "UAT_REQUESTER_SIGNED_OFF",
                    Map.of("requestId", cr.getId().toString()));
            return;
        }

        RequestUatApproverEntity approver = loadApprover(uatId, actorUserId);
        approver.approve();
        requestUatApproverRepository.save(approver);
        auditLogService.log("UAT_APPROVED", ENTITY_UAT, uatId,
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("approverUserId", actorUserId.toString(), "status", "APPROVED"), RequestContext.getCurrentIp());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_APPROVED", Map.of());
        if (cr.getCreatedBy() != null) {
            notificationService.createAndPush(cr.getCreatedBy().getId(), "UAT_APPROVAL_DECIDED",
                    actor.getFullName() + " approved UAT: " + cr.getTitle(),
                    "A UAT approver has approved.",
                    "/change-requests/" + cr.getId());
            emailService.sendUatApprovalDecisionEmail(cr.getCreatedBy().getEmail(),
                    cr.getCreatedBy().getFullName(), cr.getTitle(),
                    uat.getId().toString(), "approved",
                    actor != null ? actor.getFullName() : "Someone");
        }
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
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("reason", reason), RequestContext.getCurrentIp());
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_REJECTED", Map.of("reason", reason));
        if (cr.getCreatedBy() != null) {
            notificationService.createAndPush(cr.getCreatedBy().getId(), "UAT_APPROVAL_DECIDED",
                    actor.getFullName() + " rejected UAT: " + cr.getTitle(),
                    "A UAT approver has rejected. Reason: " + (reason != null ? reason : "No reason provided"),
                    "/change-requests/" + cr.getId());
            emailService.sendUatApprovalDecisionEmail(cr.getCreatedBy().getEmail(),
                    cr.getCreatedBy().getFullName(), cr.getTitle(),
                    uat.getId().toString(), "rejected",
                    actor != null ? actor.getFullName() : "Someone");
        }
    }

    public RequestUatEntity promoteToDeployment(UUID uatId, UUID actorUserId, String actorRole) {
        RequestUatEntity uat = loadUat(uatId);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted.");
        }

        if (!uat.isRequesterSignedOff()) {
            throw new InvalidStateTransitionException(
                    "The requester must sign-off on the UAT before promotion.");
        }

        List<RequestUatApproverEntity> approvers =
                requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId);

        List<RequestUatApproverEntity> required = approvers.stream()
                .filter(RequestUatApproverEntity::isRequired)
                .toList();

        if (!required.isEmpty()) {
            boolean allRequiredApproved = required.stream()
                    .allMatch(a -> a.getStatus() == ApproverStatus.APPROVED);
            if (!allRequiredApproved) {
                throw new InvalidStateTransitionException(
                        "All required UAT approvers must approve before promotion.");
            }
        }

        uat.setReadOnly(true);
        uat.setStatus("PROMOTED");

        RequestUatEntity saved = requestUatRepository.save(uat);

        deploymentService.createFromPromotion(uat.getRequestId(), uatId, actorUserId);

        auditLogService.log("UAT_PROMOTED", ENTITY_UAT, saved.getId(),
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("requestId", uat.getRequestId().toString()), RequestContext.getCurrentIp());
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
    public Map<UUID, UserEntity> loadApproverUsers(List<RequestUatApproverEntity> approvers) {
        List<UUID> userIds = approvers.stream()
                .map(RequestUatApproverEntity::getUserId)
                .distinct()
                .toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    @Transactional(readOnly = true)
    public String resolveUserFullName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(UserEntity::getFullName)
                .orElse(null);
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
                authorId, resolveActorEmail(authorId),
                Map.of("requestId", uat.getRequestId().toString(), "commentId", saved.getId().toString()),
                RequestContext.getCurrentIp());
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        UserEntity actor = userRepository.findById(authorId).orElse(null);
        logActivity(cr, actor, "UAT_COMMENT_ADDED", Map.of("commentId", saved.getId().toString()));
        mentionNotifier.processMentions(body, authorId, cr.getTitle(), "/change-requests/" + uat.getRequestId());
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

    private String resolveActorEmail(UUID actorUserId) {
        if (actorUserId == null) return null;
        return userRepository.findById(actorUserId).map(UserEntity::getEmail).orElse(null);
    }

    private void logActivity(ChangeRequestEntity changeRequest, UserEntity actor,
            String actionType, Map<String, Object> payload) {
        activityStreamRepository.save(new ActivityStreamEntity(changeRequest, actor, actionType, payload));
    }
}
