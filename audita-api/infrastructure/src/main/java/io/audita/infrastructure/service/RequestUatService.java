package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.exception.NotFoundException;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.CompletionStatus;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.entity.RequestUatWatcherEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestUatApproverRepository;
import io.audita.infrastructure.persistence.repository.RequestUatCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestUatRepository;
import io.audita.infrastructure.persistence.repository.RequestUatWatcherRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.tenant.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestUatService {

    private static final Logger log = LoggerFactory.getLogger(RequestUatService.class);
    private static final Set<String> ELEVATED_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final String ENTITY_UAT = "request_uat";

    private final ChangeRequestRepository changeRequestRepository;
    private final RequestUatRepository requestUatRepository;
    private final RequestUatApproverRepository requestUatApproverRepository;
    private final RequestUatWatcherRepository requestUatWatcherRepository;
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
            RequestUatWatcherRepository requestUatWatcherRepository,
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
        this.requestUatWatcherRepository = requestUatWatcherRepository;
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
        log.debug("createUat: requestId={}, actor={}", requestId, actorUserId);
        ChangeRequestEntity cr = loadRequest(requestId);
        assertCanMutate(cr, actorUserId, actorRole);
        assertNotCompleted(cr);

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
        log.info("UAT created {} for request {}", saved.getId(), requestId);
        return saved;
    }

    public RequestUatEntity updateUat(UUID uatId, String title, String details,
            UUID actorUserId, String actorRole) {
        log.debug("updateUat: uatId={}, actor={}", uatId, actorUserId);
        RequestUatEntity uat = loadUat(uatId);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);
        assertNotCompleted(cr);

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

    public RequestUatApproverEntity addApprover(UUID uatId, UUID userId,
            UUID actorUserId, String actorRole) {
        log.debug("addApprover: uatId={}, userId={}, actor={}", uatId, userId, actorUserId);
        RequestUatEntity uat = loadUat(uatId);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);
        assertNotCompleted(cr);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException(
                    "Cannot add approvers to a read-only UAT.");
        }

        if (requestUatApproverRepository.findByUatIdAndUserId(uatId, userId).isPresent()) {
            throw new DomainNotPermittedException("DUPLICATE_APPROVER",
                    "User is already an approver on this UAT.");
        }

        if (requestUatWatcherRepository.existsByUatIdAndUserId(uatId, userId)) {
            throw new DomainNotPermittedException("CONFLICT",
                    "User is already a watcher on this UAT and cannot be an approver.");
        }

        UserEntity targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        if (isAuditor(targetUser)) {
            throw new DomainNotPermittedException("FORBIDDEN",
                    "Users with the Auditor role cannot be added as UAT approvers.");
        }

        int position = requestUatApproverRepository.countByUatId(uatId) + 1;
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        approver.setUatId(uatId);
        approver.setUserId(userId);
        approver.setPosition(position);

        RequestUatApproverEntity saved = requestUatApproverRepository.save(approver);
        auditLogService.log("UAT_APPROVER_ADDED", ENTITY_UAT, uatId,
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("approverUserId", userId.toString()), RequestContext.getCurrentIp());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_APPROVER_ADDED",
                Map.of("approverUserId", userId.toString()));
        notificationService.createAndPush(userId, "UAT_APPROVER_ADDED",
                "Added as UAT approver: " + cr.getTitle(),
                "You have been added as a UAT approver.",
                "/change-requests/" + cr.getId());
        emailService.sendUatApprovalRequestEmail(targetUser.getEmail(), targetUser.getFullName(),
                cr.getTitle(), uat.getId().toString());
        log.info("Added UAT approver {} to UAT {}", userId, uatId);
        return saved;
    }

    public void approveUat(UUID uatId, UUID actorUserId) {
        log.debug("approveUat: uatId={}, actor={}", uatId, actorUserId);
        RequestUatEntity uat = loadUat(uatId);
        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted; approvals are locked.");
        }
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertNotCompleted(cr);

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
        log.debug("rejectUat: uatId={}, actor={}", uatId, actorUserId);
        RequestUatEntity uat = loadUat(uatId);
        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted; approvals are locked.");
        }
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertNotCompleted(cr);

        RequestUatApproverEntity approver = loadApprover(uatId, actorUserId);
        approver.reject(reason);
        requestUatApproverRepository.save(approver);
        auditLogService.log("UAT_REJECTED", ENTITY_UAT, uatId,
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("reason", reason), RequestContext.getCurrentIp());
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
        log.debug("promoteToDeployment: uatId={}, actor={}", uatId, actorUserId);
        RequestUatEntity uat = loadUat(uatId);
        ChangeRequestEntity cr = loadRequest(uat.getRequestId());
        assertCanMutate(cr, actorUserId, actorRole);
        assertNotCompleted(cr);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException("UAT is already promoted.");
        }

        if (!uat.isRequesterSignedOff()) {
            throw new InvalidStateTransitionException(
                    "The requester must sign-off on the UAT before promotion.");
        }

        List<RequestUatApproverEntity> approvers =
                requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId);

        // isRequired has been removed: every listed approver must approve.
        if (!approvers.isEmpty()) {
            boolean allApproved = approvers.stream()
                    .allMatch(a -> a.getStatus() == ApproverStatus.APPROVED);
            if (!allApproved) {
                throw new InvalidStateTransitionException(
                        "All UAT approvers must approve before promotion.");
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
        log.info("UAT {} promoted for request {}", saved.getId(), uat.getRequestId());
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
    public List<RequestUatWatcherEntity> listUatWatchers(UUID requestId) {
        RequestUatEntity uat = loadUatByRequestId(requestId);
        return requestUatWatcherRepository.findByUatId(uat.getId());
    }

    @Transactional
    public List<RequestUatWatcherEntity> addUatWatchers(UUID requestId, List<UUID> userIds,
            UUID actorUserId, Set<String> actorPermissions) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        log.debug("addUatWatchers: requestId={}, count={}, actor={}", requestId, userIds.size(), actorUserId);
        RequestUatEntity uat = loadUatByRequestId(requestId);
        ChangeRequestEntity cr = loadRequest(requestId);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException(
                    "Cannot add watchers to a read-only UAT.");
        }

        List<RequestUatWatcherEntity> added = new ArrayList<>();
        for (UUID userId : userIds) {
            if (requestUatWatcherRepository.existsByUatIdAndUserId(uat.getId(), userId)) {
                continue;
            }
            if (requestUatApproverRepository.findByUatIdAndUserId(uat.getId(), userId).isPresent()) {
                throw new DomainNotPermittedException("CONFLICT",
                        "User is already an approver on this UAT and cannot be a watcher.");
            }
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
            if (isAuditor(user)) {
                throw new DomainNotPermittedException("FORBIDDEN",
                        "Auditors cannot be added as UAT watchers.");
            }
            RequestUatWatcherEntity watcher = new RequestUatWatcherEntity(uat, user);
            requestUatWatcherRepository.save(watcher);
            added.add(watcher);

            auditLogService.log("UAT_WATCHER_ADDED", ENTITY_UAT, uat.getId(), actorUserId,
                    resolveActorEmail(actorUserId),
                    Map.of("watcherUserId", userId, "watcherEmail", user.getEmail()),
                    RequestContext.getCurrentIp());
            notificationService.createAndPush(userId, "UAT_WATCHER_ADDED",
                    "Added as UAT watcher: " + cr.getTitle(),
                    "You have been added as a watcher on the UAT for: " + cr.getTitle(),
                    "/change-requests/" + cr.getId());
        }
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_WATCHER_ADDED", Map.of("count", added.size()));
        log.info("Added {} UAT watchers to UAT {} (request {})", added.size(), uat.getId(), requestId);
        return added;
    }

    @Transactional
    public void removeUatWatcher(UUID requestId, UUID userId, UUID actorUserId, Set<String> actorPermissions) {
        log.debug("removeUatWatcher: requestId={}, userId={}, actor={}", requestId, userId, actorUserId);
        RequestUatEntity uat = loadUatByRequestId(requestId);
        ChangeRequestEntity cr = loadRequest(requestId);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);

        RequestUatWatcherEntity watcher = requestUatWatcherRepository
                .findByUatIdAndUserId(uat.getId(), userId)
                .orElseThrow(() -> new NotFoundException("WATCHER_NOT_FOUND", "UAT watcher not found"));
        requestUatWatcherRepository.delete(watcher);
        auditLogService.log("UAT_WATCHER_REMOVED", ENTITY_UAT, uat.getId(), actorUserId,
                resolveActorEmail(actorUserId),
                Map.of("watcherUserId", userId),
                RequestContext.getCurrentIp());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_WATCHER_REMOVED", Map.of("watcherUserId", userId));
        log.info("Removed UAT watcher {} from UAT {} (request {})", userId, uat.getId(), requestId);
    }

    @Transactional
    public void moveUatWatcherToApprover(UUID requestId, UUID userId, UUID actorUserId,
            Set<String> actorPermissions) {
        log.debug("moveUatWatcherToApprover: requestId={}, userId={}, actor={}", requestId, userId, actorUserId);
        RequestUatEntity uat = loadUatByRequestId(requestId);
        ChangeRequestEntity cr = loadRequest(requestId);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException(
                    "Cannot modify participants on a read-only UAT.");
        }

        RequestUatWatcherEntity watcher = requestUatWatcherRepository
                .findByUatIdAndUserId(uat.getId(), userId)
                .orElseThrow(() -> new NotFoundException("WATCHER_NOT_FOUND", "UAT watcher not found"));
        requestUatWatcherRepository.delete(watcher);

        int nextPosition = requestUatApproverRepository.countByUatId(uat.getId()) + 1;
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        approver.setUatId(uat.getId());
        approver.setUserId(userId);
        approver.setPosition(nextPosition);
        requestUatApproverRepository.save(approver);

        auditLogService.log("UAT_WATCHER_PROMOTED", ENTITY_UAT, uat.getId(), actorUserId,
                resolveActorEmail(actorUserId),
                Map.of("userId", userId, "newPosition", nextPosition),
                RequestContext.getCurrentIp());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_WATCHER_PROMOTED",
                Map.of("userId", userId, "newPosition", nextPosition));
        notificationService.createAndPush(userId, "UAT_WATCHER_PROMOTED",
                "Promoted to UAT approver: " + cr.getTitle(),
                "You have been promoted from watcher to approver on the UAT for: " + cr.getTitle(),
                "/change-requests/" + cr.getId());
        log.info("Moved UAT watcher {} to approver on UAT {} (request {})", userId, uat.getId(), requestId);
    }

    @Transactional
    public void moveUatApproverToWatcher(UUID requestId, UUID approverId, UUID actorUserId,
            Set<String> actorPermissions) {
        log.debug("moveUatApproverToWatcher: requestId={}, approverId={}, actor={}", requestId, approverId, actorUserId);
        RequestUatEntity uat = loadUatByRequestId(requestId);
        ChangeRequestEntity cr = loadRequest(requestId);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);

        if (uat.isReadOnly()) {
            throw new InvalidStateTransitionException(
                    "Cannot modify participants on a read-only UAT.");
        }

        RequestUatApproverEntity approver = requestUatApproverRepository.findById(approverId)
                .orElseThrow(() -> new NotFoundException("APPROVER_NOT_FOUND", "UAT approver not found"));
        if (!approver.getUatId().equals(uat.getId())) {
            throw new NotFoundException("APPROVER_NOT_FOUND", "UAT approver not found on this UAT");
        }
        if (approver.getStatus() != ApproverStatus.PENDING) {
            throw new InvalidStateTransitionException("APPROVER_ALREADY_VOTED",
                    "Cannot move an approver who has already voted.");
        }
        if (requestUatApproverRepository.countByUatId(uat.getId()) == 1) {
            throw new InvalidStateTransitionException("LAST_APPROVER_LOCKED",
                    "Cannot remove the last remaining UAT approver.");
        }

        UUID movedUserId = approver.getUserId();
        requestUatApproverRepository.delete(approver);
        resequenceUatApprovers(uat.getId());

        UserEntity movedUser = userRepository.findById(movedUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        RequestUatWatcherEntity watcher = new RequestUatWatcherEntity(uat, movedUser);
        requestUatWatcherRepository.save(watcher);

        auditLogService.log("UAT_APPROVER_DEMOTED", ENTITY_UAT, uat.getId(), actorUserId,
                resolveActorEmail(actorUserId),
                Map.of("userId", movedUserId),
                RequestContext.getCurrentIp());
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "UAT_APPROVER_DEMOTED", Map.of("userId", movedUserId));
        notificationService.createAndPush(movedUserId, "UAT_APPROVER_DEMOTED",
                "Moved to UAT watcher: " + cr.getTitle(),
                "You have been moved from approver to watcher on the UAT for: " + cr.getTitle(),
                "/change-requests/" + cr.getId());
        log.info("Moved UAT approver {} to watcher on UAT {} (request {})", approverId, uat.getId(), requestId);
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
    public Map<UUID, UserEntity> loadWatcherUsers(List<RequestUatWatcherEntity> watchers) {
        List<UUID> userIds = watchers.stream()
                .map(w -> w.getUser().getId())
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

    private RequestUatEntity loadUatByRequestId(UUID requestId) {
        return requestUatRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("UAT_NOT_FOUND", "UAT not found for request: " + requestId));
    }

    private RequestUatApproverEntity loadApprover(UUID uatId, UUID userId) {
        return requestUatApproverRepository.findByUatIdAndUserId(uatId, userId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER",
                        "You are not an approver on this UAT."));
    }

    private void resequenceUatApprovers(UUID uatId) {
        List<RequestUatApproverEntity> approvers =
                requestUatApproverRepository.findByUatIdOrderByPositionAsc(uatId);
        for (int i = 0; i < approvers.size(); i++) {
            approvers.get(i).setPosition(i + 1);
        }
        requestUatApproverRepository.saveAll(approvers);
    }

    private void assertNotCompleted(ChangeRequestEntity cr) {
        if (cr.getCompletionStatus() == CompletionStatus.COMPLETED) {
            throw new InvalidStateTransitionException("REQUEST_COMPLETED",
                    "This request is completed and read-only.");
        }
    }

    private void assertCanMutate(ChangeRequestEntity cr, UUID actorUserId, Set<String> actorPermissions) {
        if (actorPermissions != null && actorPermissions.contains("cr.view.all")) {
            return;
        }
        UserEntity createdBy = cr.getCreatedBy();
        if (createdBy == null || !createdBy.getId().equals(actorUserId)) {
            log.warn("Permission denied: actor {} cannot mutate UAT for CR {} (owner={})",
                    actorUserId, cr.getId(),
                    createdBy != null ? createdBy.getId() : null);
            throw new DomainNotPermittedException("FORBIDDEN",
                    "You are not allowed to modify this UAT record.");
        }
    }

    private void assertCanMutate(ChangeRequestEntity cr, UUID actorUserId, String actorRole) {
        Set<String> perms = new HashSet<>();
        if (actorRole != null && ELEVATED_ROLES.contains(actorRole.toUpperCase())) {
            perms.add("cr.view.all");
        }
        assertCanMutate(cr, actorUserId, perms);
    }

    private boolean isAuditor(UserEntity user) {
        if (user == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(r -> "AUDITOR".equalsIgnoreCase(r.getName()));
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
