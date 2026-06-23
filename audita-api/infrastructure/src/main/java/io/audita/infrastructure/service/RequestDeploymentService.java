package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.exception.NotFoundException;
import io.audita.domain.model.CompletionStatus;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentCommentRepository;
import io.audita.infrastructure.persistence.repository.RequestDeploymentRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.tenant.RequestContext;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(RequestDeploymentService.class);
    private static final Set<String> ELEVATED_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final String ENTITY_DEPLOYMENT = "request_deployment";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final RequestDeploymentRepository deploymentRepository;
    private final RequestDeploymentCommentRepository deploymentCommentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ActivityStreamRepository activityStreamRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final NotificationService notificationService;
    private final MentionNotifier mentionNotifier;

    public RequestDeploymentService(RequestDeploymentRepository deploymentRepository,
            RequestDeploymentCommentRepository deploymentCommentRepository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            ActivityStreamRepository activityStreamRepository,
            ChangeRequestRepository changeRequestRepository,
            NotificationService notificationService,
            MentionNotifier mentionNotifier) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentCommentRepository = deploymentCommentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.activityStreamRepository = activityStreamRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.notificationService = notificationService;
        this.mentionNotifier = mentionNotifier;
    }

    public RequestDeploymentEntity createFromPromotion(UUID requestId, UUID uatId, UUID actorUserId) {
        log.debug("createFromPromotion: requestId={}, uatId={}, actor={}", requestId, uatId, actorUserId);
        if (deploymentRepository.existsByRequestId(requestId)) {
            throw new InvalidStateTransitionException(
                    "A deployment already exists for this request.");
        }

        RequestDeploymentEntity deployment = new RequestDeploymentEntity();
        deployment.setRequestId(requestId);
        deployment.setUatId(uatId);
        deployment.setCreatedBy(actorUserId);
        deployment.setStatus(STATUS_PENDING);
        deployment.setPromotedAt(OffsetDateTime.now());

        RequestDeploymentEntity saved = deploymentRepository.save(deployment);

        auditLogService.log("DEPLOYMENT_CREATED", ENTITY_DEPLOYMENT, saved.getId(),
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("requestId", requestId.toString(), "uatId", uatId.toString()),
                RequestContext.getCurrentIp());

        ChangeRequestEntity cr = changeRequestRepository.findById(requestId).orElse(null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "DEPLOYMENT_CREATED",
                Map.of("requestId", requestId.toString(), "uatId", uatId.toString()));

        log.info("Deployment {} created for request {} (promotion from UAT {})",
                saved.getId(), requestId, uatId);
        return saved;
    }

    @Transactional
    public RequestDeploymentEntity assignDeployer(UUID requestId, UUID deployerUserId,
            UUID actorUserId, String actorRole) {
        return assignDeployer(requestId, deployerUserId, actorUserId, permissionsForRole(actorRole));
    }

    @Transactional
    public RequestDeploymentEntity assignDeployer(UUID requestId, UUID deployerUserId,
            UUID actorUserId, Set<String> actorPermissions) {
        log.debug("assignDeployer: requestId={}, deployer={}, actor={}", requestId, deployerUserId, actorUserId);
        RequestDeploymentEntity deployment = loadDeploymentByRequestId(requestId);
        assertNotCompleted(requestId);
        assertCanManageParticipants(deployment, actorUserId, actorPermissions);

        UserEntity deployer = userRepository.findById(deployerUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (isAuditor(deployer)) {
            throw new DomainNotPermittedException("FORBIDDEN",
                    "Auditors cannot be assigned as deployers.");
        }

        UUID previousAssigneeId = deployment.getAssignee() != null
                ? deployment.getAssignee().getId() : null;
        deployment.setAssignee(deployer);

        auditLogService.log(
                previousAssigneeId == null ? "DEPLOYMENT_ASSIGNEE_SET" : "DEPLOYMENT_ASSIGNEE_CHANGED",
                ENTITY_DEPLOYMENT, deployment.getId(), actorUserId, resolveActorEmail(actorUserId),
                Map.of(
                        "assigneeId", deployerUserId.toString(),
                        "assigneeEmail", deployer.getEmail(),
                        "previousAssigneeId", previousAssigneeId == null ? "null" : previousAssigneeId.toString()),
                RequestContext.getCurrentIp());

        ChangeRequestEntity cr = changeRequestRepository.findById(requestId).orElse(null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor,
                previousAssigneeId == null ? "DEPLOYMENT_ASSIGNEE_SET" : "DEPLOYMENT_ASSIGNEE_CHANGED",
                Map.of("assigneeId", deployerUserId.toString()));

        notificationService.createAndPush(deployerUserId, "DEPLOYMENT_ASSIGNED",
                "Deployment Assigned", "You have been assigned to handle a deployment.",
                "/change-requests/" + requestId);

        log.info("Deployer assigned to deployment {} for request {}: {}",
                deployment.getId(), requestId, deployer.getEmail());
        initializeAssignee(deployment);
        return deployment;
    }

    @Transactional
    public RequestDeploymentEntity completeDeployment(UUID requestId, UUID actorUserId,
            String actorRole) {
        return completeDeployment(requestId, actorUserId, permissionsForRole(actorRole));
    }

    @Transactional
    public RequestDeploymentEntity completeDeployment(UUID requestId, UUID actorUserId,
            Set<String> actorPermissions) {
        log.debug("completeDeployment: requestId={}, actor={}", requestId, actorUserId);
        RequestDeploymentEntity deployment = loadDeploymentByRequestId(requestId);

        if (!STATUS_PENDING.equals(deployment.getStatus())) {
            throw new InvalidStateTransitionException("DEPLOYMENT_NOT_PENDING",
                    "Deployment is not in PENDING status.");
        }
        if (deployment.getAssignee() == null) {
            throw new InvalidStateTransitionException("NO_ASSIGNEE",
                    "Deployment has no assignee.");
        }

        boolean isAssignee = deployment.getAssignee().getId().equals(actorUserId);
        boolean isAdmin = actorPermissions != null && actorPermissions.contains("cr.view.all");
        if (!isAssignee && !isAdmin) {
            throw new DomainNotPermittedException("FORBIDDEN",
                    "Only the assigned deployer can mark the deployment as completed.");
        }

        deployment.setStatus(STATUS_COMPLETED);
        deployment.setCompletedAt(OffsetDateTime.now());

        auditLogService.log("DEPLOYMENT_COMPLETED", ENTITY_DEPLOYMENT,
                deployment.getId(), actorUserId, resolveActorEmail(actorUserId),
                Map.of(
                        "assigneeId", deployment.getAssignee().getId().toString(),
                        "completedAt", deployment.getCompletedAt().toString()),
                RequestContext.getCurrentIp());

        ChangeRequestEntity cr = changeRequestRepository.findById(requestId).orElse(null);
        UserEntity actor = userRepository.findById(actorUserId).orElse(null);
        logActivity(cr, actor, "DEPLOYMENT_COMPLETED", Map.of());

        if (cr != null && cr.getCreatedBy() != null) {
            notificationService.createAndPush(cr.getCreatedBy().getId(), "DEPLOYMENT_COMPLETED",
                    "Deployment Completed",
                    "The deployment for \"" + cr.getTitle() + "\" has been marked complete.",
                    "/change-requests/" + requestId);
        }

        log.info("Deployment {} completed for request {} by {}",
                deployment.getId(), requestId, resolveActorEmail(actorUserId));
        initializeAssignee(deployment);
        return deployment;
    }

    @Transactional(readOnly = true)
    public Optional<RequestDeploymentEntity> getByRequestId(UUID requestId) {
        return deploymentRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public Optional<RequestDeploymentEntity> getDeploymentWithAssignee(UUID requestId) {
        return deploymentRepository.findByRequestId(requestId)
                .map(this::initializeAssignee);
    }

    @Transactional(readOnly = true)
    public String resolveUserFullName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(UserEntity::getFullName)
                .orElse(null);
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
        RequestDeploymentCommentEntity saved = deploymentCommentRepository.save(
                new RequestDeploymentCommentEntity(deploymentId, authorId, body));
        auditLogService.log("DEPLOYMENT_COMMENT_ADDED", ENTITY_DEPLOYMENT, deploymentId,
                authorId, resolveActorEmail(authorId),
                Map.of("requestId", deployment.getRequestId().toString(), "commentId", saved.getId().toString()),
                RequestContext.getCurrentIp());
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
        List<UserEntity> users = userRepository.findAllById(authorIds);
        users.forEach(u -> {
            Hibernate.initialize(u.getRole());
            Hibernate.initialize(u.getRoles());
        });
        return users.stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    @Transactional(readOnly = true)
    public boolean isDeploymentDone(UUID requestId) {
        return deploymentRepository.findByRequestId(requestId)
                .map(d -> STATUS_COMPLETED.equals(d.getStatus()))
                .orElse(false);
    }

    private RequestDeploymentEntity initializeAssignee(RequestDeploymentEntity deployment) {
        UserEntity assignee = deployment.getAssignee();
        if (assignee != null) {
            assignee.getFullName();
            assignee.getEmail();
        }
        return deployment;
    }

    private RequestDeploymentEntity loadDeployment(UUID deploymentId) {
        return deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new NotFoundException("DEPLOYMENT", deploymentId));
    }

    private RequestDeploymentEntity loadDeploymentByRequestId(UUID requestId) {
        return deploymentRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("DEPLOYMENT", requestId));
    }

    private void assertNotCompleted(UUID requestId) {
        ChangeRequestEntity cr = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("CHANGE_REQUEST", requestId));
        if (cr.getCompletionStatus() == CompletionStatus.COMPLETED) {
            throw new InvalidStateTransitionException("REQUEST_COMPLETED",
                    "This request is completed and read-only.");
        }
    }

    private void assertCanManageParticipants(RequestDeploymentEntity deployment, UUID actorUserId,
            Set<String> actorPermissions) {
        if (actorPermissions != null && actorPermissions.contains("cr.view.all")) {
            return;
        }
        ChangeRequestEntity cr = changeRequestRepository.findById(deployment.getRequestId())
                .orElseThrow(() -> new NotFoundException("CHANGE_REQUEST", deployment.getRequestId()));
        UserEntity createdBy = cr.getCreatedBy();
        if (createdBy == null || !createdBy.getId().equals(actorUserId)) {
            log.warn("Permission denied: actor {} cannot manage deployment participants for CR {} (owner={})",
                    actorUserId, cr.getId(), createdBy != null ? createdBy.getId() : null);
            throw new DomainNotPermittedException("FORBIDDEN",
                    "You are not allowed to manage this deployment's assignee.");
        }
    }

    private Set<String> permissionsForRole(String actorRole) {
        Set<String> perms = new HashSet<>();
        if (actorRole != null && ELEVATED_ROLES.contains(actorRole.toUpperCase())) {
            perms.add("cr.view.all");
        }
        return perms;
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
