package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidRequestException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.exception.NotFoundException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.CompletionStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.domain.model.RiskLevel;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.AttachmentEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldEntity;
import io.audita.infrastructure.persistence.entity.CustomFieldDefinitionEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.CrWatcherEntity;
import io.audita.infrastructure.persistence.entity.OrgSettingEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.AttachmentRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestCustomFieldRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CustomFieldDefinitionRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.CrWatcherRepository;
import io.audita.infrastructure.persistence.repository.GroupRepository;
import io.audita.infrastructure.persistence.repository.GroupMemberRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.security.HtmlSanitizer;
import io.audita.infrastructure.tenant.RequestContext;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@Transactional
public class ChangeRequestService {

    private static final Logger log = LoggerFactory.getLogger(ChangeRequestService.class);
    private static final Set<String> ELEVATED_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final Set<String> GLOBAL_VIEW_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "AUDITOR");
    private static final String ERROR_NOT_FOUND = "NOT_FOUND";
    private static final String PAYLOAD_STATUS = "status";
    private static final String PAYLOAD_COUNT = "count";
    private static final String ENTITY_CHANGE_REQUEST = "change_request";
    private static final String PAYLOAD_APPROVER_ID = "approverId";
    private static final String WORKFLOW_DEFAULT_APPROVER_USER_IDS_KEY = "workflow.default_approver_user_ids";
    private static final String WORKFLOW_DEFAULT_APPROVER_GROUP_IDS_KEY = "workflow.default_approver_group_ids";
    private static final String REQUEST_ID_PREFIX_KEY = "request.id_prefix";
    private static final String REQUEST_ID_SEQUENCE_KEY = "request.id_sequence";

    private final ChangeRequestRepository changeRequestRepository;
    private final CrApproverRepository crApproverRepository;
    private final CrWatcherRepository crWatcherRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ChangeRequestCustomFieldRepository customFieldRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final OrgSettingRepository orgSettingRepository;
    private final RequestDeploymentService deploymentService;
    private final AuditLogService auditLogService;
    private final HtmlSanitizer htmlSanitizer;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;

    @Value("${audita.storage.local.base-path:/tmp/uploads}")
    private String storageBasePath;

    @Value("${audita.upload.max-size-bytes:10485760}")
    private long maxUploadSizeBytes;

    @Value("${audita.upload.allowed-mime-types:application/pdf,image/png,image/jpeg,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}")
    private String allowedMimeTypes;

    public ChangeRequestService(ChangeRequestRepository changeRequestRepository,
            CrApproverRepository crApproverRepository,
            CrWatcherRepository crWatcherRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            ChangeRequestCustomFieldRepository customFieldRepository,
            CustomFieldDefinitionRepository customFieldDefinitionRepository,
            ActivityStreamRepository activityStreamRepository,
            AttachmentRepository attachmentRepository,
            UserRepository userRepository,
            OrgSettingRepository orgSettingRepository,
            RequestDeploymentService deploymentService,
            AuditLogService auditLogService,
            HtmlSanitizer htmlSanitizer,
            NotificationService notificationService,
            EmailService emailService) {
        this.changeRequestRepository = changeRequestRepository;
        this.crApproverRepository = crApproverRepository;
        this.crWatcherRepository = crWatcherRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.customFieldRepository = customFieldRepository;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.orgSettingRepository = orgSettingRepository;
        this.deploymentService = deploymentService;
        this.auditLogService = auditLogService;
        this.htmlSanitizer = htmlSanitizer;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    public ChangeRequestEntity create(CreateRequest request) {
        log.debug("create: title={}, actor={}", request.title(), request.createdById());
        UserEntity createdBy = userRepository.findById(request.createdById())
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "User not found."));

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setTitle(request.title());
        changeRequest.setDescription(sanitizeRichText(request.description()));
        changeRequest.setPriority(request.priority());
        changeRequest.setRiskLevel(request.riskLevel());
        changeRequest.setCategory(request.category());
        changeRequest.setApprovalType(
                request.approvalType() != null ? request.approvalType() : ApprovalType.NON_LINEAR);
        changeRequest.setScheduledStart(request.scheduledStart());
        changeRequest.setScheduledEnd(request.scheduledEnd());
        changeRequest.setCreatedBy(createdBy);
        changeRequest.setAffectedSystems(normalizeAffectedSystems(request.affectedSystems()));
        changeRequest.setDisplayId(generateDisplayId());
        if (request.workflowMode() != null) {
            changeRequest.setWorkflowMode(request.workflowMode());
        }
        changeRequest.setRequestDepartmentId(request.requestDepartmentId());
        changeRequest.setDestinationDepartmentId(request.destinationDepartmentId());
        changeRequest.setRequestGroupId(request.requestGroupId());
        changeRequest.setDestinationGroupId(request.destinationGroupId());
        ChangeRequestEntity created = changeRequestRepository.save(changeRequest);
        ensureDefaultApprovers(created);
        logActivity(created, createdBy, "CR_CREATED", Map.of(PAYLOAD_STATUS, created.getStatus().name()));
        auditLogService.log("CR_CREATED", ENTITY_CHANGE_REQUEST, created.getId(),
                createdBy.getId(), createdBy.getEmail(),
                Map.of("title", created.getTitle(), "priority", created.getPriority().name()), RequestContext.getCurrentIp());
        initializeCreator(created);
        return created;
    }

    public ChangeRequestEntity update(UpdateRequest request) {
        ChangeRequestEntity current = getById(request.id());
        assertCanMutate(current, request.actorUserId(), request.actorRole());
        assertNotCompleted(current);
        if (current.getStatus() != ChangeRequestStatus.DRAFT) {
            throw new InvalidStateTransitionException("CR_NOT_EDITABLE",
                    "Change request can only be edited while in DRAFT status.");
        }

        if (request.title() != null) {
            current.setTitle(request.title());
        }
        if (request.description() != null) {
            current.setDescription(sanitizeRichText(request.description()));
        }
        if (request.priority() != null) {
            current.setPriority(request.priority());
        }
        if (request.riskLevel() != null) {
            current.setRiskLevel(request.riskLevel());
        }
        if (request.category() != null) {
            current.setCategory(request.category());
        }
        if (request.approvalType() != null) {
            if (current.isApprovalLocked()) {
                throw new InvalidStateTransitionException("Approval type is locked and cannot be changed.");
            }
            current.setApprovalType(request.approvalType());
        }
        if (request.scheduledStart() != null) {
            current.setScheduledStart(request.scheduledStart());
        }
        if (request.scheduledEnd() != null) {
            current.setScheduledEnd(request.scheduledEnd());
        }
        if (request.affectedSystems() != null) {
            current.setAffectedSystems(normalizeAffectedSystems(request.affectedSystems()));
        }
        if (request.workflowMode() != null) {
            current.setWorkflowMode(request.workflowMode());
        }
        if (request.requestDepartmentId() != null) {
            current.setRequestDepartmentId(request.requestDepartmentId());
        }
        if (request.destinationDepartmentId() != null) {
            current.setDestinationDepartmentId(request.destinationDepartmentId());
        }
        if (request.requestGroupId() != null) {
            current.setRequestGroupId(request.requestGroupId());
        }
        if (request.destinationGroupId() != null) {
            current.setDestinationGroupId(request.destinationGroupId());
        }

        ChangeRequestEntity updated = changeRequestRepository.save(current);
        logActivity(updated, updated.getCreatedBy(), "CR_UPDATED", Map.of(PAYLOAD_STATUS, updated.getStatus().name()));
        initializeCreator(updated);
        return updated;
    }

    public ChangeRequestEntity submit(UUID id, UUID actorUserId, String actorRole) {
        log.info("CR submit: id={}, actor={}", id, actorUserId);
        ChangeRequestEntity changeRequest = getById(id);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        assertNotCompleted(changeRequest);
        ensureDefaultApprovers(changeRequest);
        if (crApproverRepository.countByChangeRequestId(changeRequest.getId()) < 1) {
            throw new InvalidStateTransitionException("APPROVERS_REQUIRED",
                    "Add at least one approver before submitting this change request.");
        }
        changeRequest.submit();
        changeRequest.setSlaDeadline(OffsetDateTime.now().plusHours(resolveSlaHours(changeRequest.getPriority())));
        ChangeRequestEntity submitted = changeRequestRepository.save(changeRequest);
        logActivity(submitted, submitted.getCreatedBy(), "CR_SUBMITTED",
                Map.of(PAYLOAD_STATUS, submitted.getStatus().name()));
        auditLogService.log("CR_SUBMITTED", ENTITY_CHANGE_REQUEST, submitted.getId(),
                actorUserId, submitted.getCreatedBy() != null ? submitted.getCreatedBy().getEmail() : null,
                Map.of(PAYLOAD_STATUS, "PENDING_APPROVAL"), RequestContext.getCurrentIp());
        List<CrApproverEntity> submitApprovers = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(submitted.getId());
        for (CrApproverEntity approver : submitApprovers) {
            notificationService.createAndPush(approver.getUser().getId(), "APPROVAL_REQUESTED",
                    "Approval needed: " + submitted.getTitle(),
                    "Your review is needed for this change request.",
                    "/change-requests/" + submitted.getId());
            emailService.sendApprovalRequestEmail(approver.getUser().getEmail(),
                    approver.getUser().getFullName(), submitted.getTitle(), submitted.getId().toString());
        }
        initializeCreator(submitted);
        return submitted;
    }

    public void cancel(UUID id, UUID actorUserId, String actorRole) {
        log.info("CR cancel: id={}, actor={}", id, actorUserId);
        ChangeRequestEntity changeRequest = getById(id);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        assertNotCompleted(changeRequest);
        changeRequest.cancel();
        ChangeRequestEntity cancelled = changeRequestRepository.save(changeRequest);
        logActivity(cancelled, cancelled.getCreatedBy(), "CR_CANCELLED",
                Map.of(PAYLOAD_STATUS, cancelled.getStatus().name()));
        auditLogService.log("CR_CANCELLED", ENTITY_CHANGE_REQUEST, cancelled.getId(),
                actorUserId, cancelled.getCreatedBy() != null ? cancelled.getCreatedBy().getEmail() : null,
                Map.of(PAYLOAD_STATUS, "CANCELLED"), RequestContext.getCurrentIp());
        List<CrApproverEntity> cancelApprovers = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(cancelled.getId());
        for (CrApproverEntity approver : cancelApprovers) {
            notificationService.createAndPush(approver.getUser().getId(), "CR_CANCELLED",
                    "Request cancelled: " + cancelled.getTitle(),
                    "This change request has been cancelled.",
                    "/change-requests/" + cancelled.getId());
            emailService.sendCrCancelledEmail(approver.getUser().getEmail(),
                    approver.getUser().getFullName(), cancelled.getTitle(),
                    cancelled.getId().toString(),
                    cancelled.getCreatedBy() != null ? cancelled.getCreatedBy().getFullName() : "Someone");
        }
    }

    public ChangeRequestEntity completeRequest(UUID requestId, UUID actorUserId, String actorRole) {
        log.info("CR completeRequest: id={}, actor={}", requestId, actorUserId);
        ChangeRequestEntity cr = getById(requestId);
        assertCanMutate(cr, actorUserId, actorRole);
        assertNotCompleted(cr);

        if (cr.getWorkflowMode() == RequestWorkflowMode.APPROVAL_ONLY) {
            if (cr.getApprovalStatus() != ChangeRequestStatus.APPROVED) {
                throw new InvalidStateTransitionException(
                        "Approval-only requests must be approved before completion.");
            }
        } else if (cr.getWorkflowMode() == RequestWorkflowMode.DELIVERY_PIPELINE) {
            if (cr.getApprovalStatus() != ChangeRequestStatus.APPROVED) {
                throw new InvalidStateTransitionException(
                        "Delivery pipeline requests must be approved before completion.");
            }
            if (!deploymentService.isDeploymentDone(requestId)) {
                throw new InvalidStateTransitionException(
                        "Deployment must be fully approved before the request can be completed.");
            }
        }

        cr.setCompletionStatus(CompletionStatus.COMPLETED);
        ChangeRequestEntity updated = changeRequestRepository.save(cr);
        logActivity(updated, updated.getCreatedBy(), "REQ_COMPLETION_STATUS_CHANGED",
                Map.of("completionStatus", "COMPLETED"));
        auditLogService.log("REQ_COMPLETION_STATUS_CHANGED", ENTITY_CHANGE_REQUEST, updated.getId(),
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("completionStatus", "COMPLETED", "workflowMode", updated.getWorkflowMode().name()), RequestContext.getCurrentIp());
        List<CrApproverEntity> completeApprovers = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(updated.getId());
        for (CrApproverEntity approver : completeApprovers) {
            notificationService.createAndPush(approver.getUser().getId(), "CR_COMPLETED",
                    "Request completed: " + updated.getTitle(),
                    "This change request has been completed.",
                    "/change-requests/" + updated.getId());
            emailService.sendCrCompletedEmail(approver.getUser().getEmail(),
                    approver.getUser().getFullName(), updated.getTitle(), updated.getId().toString());
        }
        initializeCreator(updated);
        return updated;
    }

    public ChangeRequestEntity setWorkflowMode(UUID requestId, RequestWorkflowMode mode,
            UUID actorUserId, String actorRole) {
        if (mode == null) {
            throw new InvalidRequestException("INVALID_INPUT", "Workflow mode is required.");
        }
        ChangeRequestEntity cr = getById(requestId);
        assertCanMutate(cr, actorUserId, actorRole);
        assertNotCompleted(cr);

        if (cr.getStatus() != ChangeRequestStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Workflow mode can only be changed while the request is in DRAFT status.");
        }

        cr.setWorkflowMode(mode);
        ChangeRequestEntity updated = changeRequestRepository.save(cr);
        logActivity(updated, updated.getCreatedBy(), "REQ_WORKFLOW_MODE_CHANGED",
                Map.of("workflowMode", mode.name()));
        auditLogService.log("REQ_WORKFLOW_MODE_CHANGED", ENTITY_CHANGE_REQUEST, updated.getId(),
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("workflowMode", mode.name()), RequestContext.getCurrentIp());
        initializeCreator(updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequestEntity> list(ChangeRequestStatus status,
            Priority priority,
            String category,
            UUID createdBy,
            UUID viewerId,
            String viewerRole,
            Pageable pageable) {
        boolean viewerHasGlobalAccess = hasGlobalViewAccess(viewerRole);
        return changeRequestRepository.findAllFiltered(
                status,
                priority,
                category,
                createdBy,
                viewerId,
                viewerHasGlobalAccess,
                ChangeRequestStatus.DRAFT,
                pageable)
                .map(changeRequest -> {
                    initializeCreator(changeRequest);
                    return changeRequest;
                });
    }

    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return changeRequestRepository.findDistinctCategories().stream()
                .flatMap(raw -> java.util.Arrays.stream(raw.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public ChangeRequestEntity getById(UUID id, UUID viewerId, String viewerRole) {
        ChangeRequestEntity changeRequest = getById(id);
        if (!hasGlobalViewAccess(viewerRole) && !isViewerAllowed(changeRequest, viewerId)) {
            throw new DomainNotPermittedException(ERROR_NOT_FOUND, "Change request not found.");
        }
        // Draft CRs are private to their creator. Return NOT_FOUND (not 403) to
        // avoid leaking that the resource exists to users who cannot see it.
        if (changeRequest.getStatus() == ChangeRequestStatus.DRAFT &&
                (changeRequest.getCreatedBy() == null || !changeRequest.getCreatedBy().getId().equals(viewerId))) {
            throw new DomainNotPermittedException(ERROR_NOT_FOUND, "Change request not found.");
        }
        return changeRequest;
    }

    @Transactional(readOnly = true)
    public ChangeRequestEntity getById(UUID id, UUID viewerId, Set<String> viewerPermissions) {
        ChangeRequestEntity changeRequest = getById(id);
        if (!hasGlobalViewAccess(viewerPermissions) && !isViewerAllowed(changeRequest, viewerId)) {
            throw new DomainNotPermittedException(ERROR_NOT_FOUND, "Change request not found.");
        }
        if (changeRequest.getStatus() == ChangeRequestStatus.DRAFT &&
                (changeRequest.getCreatedBy() == null || !changeRequest.getCreatedBy().getId().equals(viewerId))) {
            throw new DomainNotPermittedException(ERROR_NOT_FOUND, "Change request not found.");
        }
        return changeRequest;
    }

    // Internal lookup — skips draft-visibility check. Only call from within service
    // methods that already enforce mutation authorization (assertCanMutate).
    private ChangeRequestEntity getById(UUID id) {
        ChangeRequestEntity changeRequest = changeRequestRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "Change request not found."));
        initializeCreator(changeRequest);
        return changeRequest;
    }

    @Transactional(readOnly = true)
    public List<CrApproverEntity> listApprovers(UUID changeRequestId) {
        getById(changeRequestId);
        return crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId);
    }

    @Transactional(readOnly = true)
    public List<CrWatcherEntity> listWatchers(UUID crId) {
        getById(crId);
        return crWatcherRepository.findByChangeRequestId(crId);
    }

    @Transactional
    public List<CrWatcherEntity> addWatchers(UUID crId,
            List<UUID> userIds,
            UUID actorUserId,
            Set<String> actorPermissions) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        log.debug("addWatchers: crId={}, count={}, actor={}", crId, userIds.size(), actorUserId);
        ChangeRequestEntity cr = getById(crId, actorUserId, actorPermissions);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);

        List<CrWatcherEntity> added = new ArrayList<>();
        for (UUID userId : userIds) {
            if (crWatcherRepository.existsByChangeRequestIdAndUserId(crId, userId)) {
                continue;
            }
            if (cr.getApprovers().stream().anyMatch(a -> a.getUser().getId().equals(userId))) {
                throw new DomainNotPermittedException("CONFLICT",
                        "User is already an approver on this request and cannot be a watcher.");
            }
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
            if (isAuditor(user)) {
                throw new DomainNotPermittedException("FORBIDDEN",
                        "Auditors cannot be added as watchers.");
            }
            CrWatcherEntity watcher = new CrWatcherEntity(cr, user);
            crWatcherRepository.save(watcher);
            added.add(watcher);

            auditLogService.log("CR_WATCHER_ADDED", ENTITY_CHANGE_REQUEST, crId, actorUserId,
                    resolveActorEmail(actorUserId),
                    Map.of("watcherUserId", userId, "watcherEmail", user.getEmail()),
                    RequestContext.getCurrentIp());
            logActivity(cr, cr.getCreatedBy(), "CR_WATCHER_ADDED",
                    Map.of("watcherUserId", userId, "watcherEmail", user.getEmail()));
            notificationService.createAndPush(userId, "CR_WATCHER_ADDED",
                    "Added as Watcher", "You have been added as a watcher to: " + cr.getTitle(),
                    "/change-requests/" + crId);
        }
        log.info("Added {} watchers to CR {}", added.size(), crId);
        return added;
    }

    @Transactional
    public void removeWatcher(UUID crId, UUID userId, UUID actorUserId, Set<String> actorPermissions) {
        log.debug("removeWatcher: crId={}, userId={}, actor={}", crId, userId, actorUserId);
        ChangeRequestEntity cr = getById(crId, actorUserId, actorPermissions);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);
        CrWatcherEntity watcher = crWatcherRepository.findByCrIdAndUserId(crId, userId)
                .orElseThrow(() -> new NotFoundException("WATCHER_NOT_FOUND", "Watcher not found"));
        crWatcherRepository.delete(watcher);
        auditLogService.log("CR_WATCHER_REMOVED", ENTITY_CHANGE_REQUEST, crId, actorUserId,
                resolveActorEmail(actorUserId),
                Map.of("watcherUserId", userId),
                RequestContext.getCurrentIp());
        logActivity(cr, cr.getCreatedBy(), "CR_WATCHER_REMOVED",
                Map.of("watcherUserId", userId));
        log.info("Removed watcher {} from CR {}", userId, crId);
    }

    @Transactional
    public void moveWatcherToApprover(UUID crId, UUID userId, UUID actorUserId, Set<String> actorPermissions) {
        log.debug("moveWatcherToApprover: crId={}, userId={}, actor={}", crId, userId, actorUserId);
        ChangeRequestEntity cr = getById(crId, actorUserId, actorPermissions);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);
        CrWatcherEntity watcher = crWatcherRepository.findByCrIdAndUserId(crId, userId)
                .orElseThrow(() -> new NotFoundException("WATCHER_NOT_FOUND", "Watcher not found"));
        UserEntity promotedUser = watcher.getUser();
        crWatcherRepository.delete(watcher);

        int nextPosition = cr.getApprovers().stream()
                .mapToInt(CrApproverEntity::getPosition)
                .max()
                .orElse(0) + 1;
        CrApproverEntity approver = new CrApproverEntity(cr, promotedUser, nextPosition);
        approver.setStatus(ApproverStatus.PENDING);
        crApproverRepository.save(approver);

        auditLogService.log("CR_WATCHER_PROMOTED", ENTITY_CHANGE_REQUEST, crId, actorUserId,
                resolveActorEmail(actorUserId),
                Map.of("userId", userId, "newPosition", nextPosition),
                RequestContext.getCurrentIp());
        logActivity(cr, cr.getCreatedBy(), "CR_WATCHER_PROMOTED",
                Map.of("userId", userId, "newPosition", nextPosition));
        log.info("Moved watcher {} to approver on CR {}", userId, crId);
    }

    @Transactional
    public void moveApproverToWatcher(UUID crId, UUID approverId, UUID actorUserId, Set<String> actorPermissions) {
        log.debug("moveApproverToWatcher: crId={}, approverId={}, actor={}", crId, approverId, actorUserId);
        ChangeRequestEntity cr = getById(crId, actorUserId, actorPermissions);
        assertCanMutate(cr, actorUserId, actorPermissions);
        assertNotCompleted(cr);
        CrApproverEntity approver = cr.getApprovers().stream()
                .filter(a -> a.getId().equals(approverId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("APPROVER_NOT_FOUND", "Approver not found"));

        if (approver.getStatus() != ApproverStatus.PENDING) {
            throw new InvalidStateTransitionException("APPROVER_ALREADY_VOTED",
                    "Cannot move an approver who has already voted.");
        }

        UserEntity movedUser = approver.getUser();
        UUID movedUserId = movedUser.getId();
        cr.getApprovers().remove(approver);
        crApproverRepository.delete(approver);
        resequenceApprovers(crId);

        CrWatcherEntity watcher = new CrWatcherEntity(cr, movedUser);
        crWatcherRepository.save(watcher);

        auditLogService.log("CR_APPROVER_DEMOTED", ENTITY_CHANGE_REQUEST, crId, actorUserId,
                resolveActorEmail(actorUserId),
                Map.of("userId", movedUserId),
                RequestContext.getCurrentIp());
        logActivity(cr, cr.getCreatedBy(), "CR_APPROVER_DEMOTED",
                Map.of("userId", movedUserId));
        log.info("Moved approver {} to watcher on CR {}", approverId, crId);
    }

    public CrApproverEntity addApprover(UUID changeRequestId,
            UUID userId,
            UUID actorUserId,
            String actorRole) {
        log.debug("addApprover: crId={}, userId={}, actor={}", changeRequestId, userId, actorUserId);
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        assertNotCompleted(changeRequest);
        assertApproverManagementAllowed(changeRequest);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "User not found."));

        if (user.getRoles().stream().anyMatch(r -> "AUDITOR".equalsIgnoreCase(r.getName()))) {
            throw new DomainNotPermittedException("FORBIDDEN",
                    "Users with the Auditor role cannot be added as approvers.");
        }

        if (crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, userId).isPresent()) {
            throw new DomainNotPermittedException("DUPLICATE_APPROVER", "User is already an approver on this CR.");
        }

        if (crWatcherRepository.existsByChangeRequestIdAndUserId(changeRequestId, userId)) {
            throw new DomainNotPermittedException("CONFLICT",
                    "User is already a watcher on this request and cannot be an approver.");
        }

        int position = crApproverRepository.countByChangeRequestId(changeRequestId) + 1;
        CrApproverEntity approver = new CrApproverEntity(changeRequest, user, position);
        CrApproverEntity created = crApproverRepository.save(approver);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVER_ADDED",
                Map.of(PAYLOAD_APPROVER_ID, created.getId().toString(), "userId", userId.toString()));
        auditLogService.log("CR_APPROVER_ADDED", ENTITY_CHANGE_REQUEST, changeRequestId,
                actorUserId, resolveActorEmail(actorUserId),
                Map.of(PAYLOAD_APPROVER_ID, created.getId().toString(), "userId", userId.toString()),
                RequestContext.getCurrentIp());
        notificationService.createAndPush(user.getId(), "APPROVER_ADDED",
                "Added as approver: " + changeRequest.getTitle(),
                "You have been added as an approver.",
                "/change-requests/" + changeRequestId);
        emailService.sendApprovalRequestEmail(user.getEmail(), user.getFullName(),
                changeRequest.getTitle(), changeRequestId.toString());
        return crApproverRepository.findWithUserAndRolesById(created.getId())
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "Approver not found."));
    }

    public List<CrApproverEntity> addApproverGroup(UUID changeRequestId,
            UUID groupId,
            boolean isRequired,
            UUID actorUserId,
            String actorRole) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        assertNotCompleted(changeRequest);
        assertApproverManagementAllowed(changeRequest);

        if (!groupRepository.existsById(groupId)) {
            throw new DomainNotPermittedException(ERROR_NOT_FOUND, "Group not found.");
        }

        List<UserEntity> groupUsers = groupMemberRepository
                .findDistinctUsersByGroupIdInAndUserStatus(List.of(groupId), UserStatus.ACTIVE);
        if (groupUsers.isEmpty()) {
            return List.of();
        }

        List<CrApproverEntity> existing = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        Set<UUID> existingUserIds = existing.stream()
                .map(entry -> entry.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());

        int nextPosition = existing.size() + 1;
        List<CrApproverEntity> additions = new java.util.ArrayList<>();
        for (UserEntity user : groupUsers.stream().sorted(java.util.Comparator.comparing(UserEntity::getFullName))
                .toList()) {
            if (existingUserIds.contains(user.getId())) {
                continue;
            }
            additions.add(new CrApproverEntity(changeRequest, user, nextPosition++));
        }
        if (additions.isEmpty()) {
            return List.of();
        }

        List<CrApproverEntity> saved = crApproverRepository.saveAll(additions);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVER_GROUP_ADDED",
                Map.of("groupId", groupId.toString(), PAYLOAD_COUNT, saved.size()));
        auditLogService.log("CR_APPROVER_GROUP_ADDED", ENTITY_CHANGE_REQUEST, changeRequestId,
                actorUserId, resolveActorEmail(actorUserId),
                Map.of("groupId", groupId.toString(), PAYLOAD_COUNT, saved.size(), "isRequired", isRequired),
                RequestContext.getCurrentIp());
        for (CrApproverEntity a : saved) {
            notificationService.createAndPush(a.getUser().getId(), "APPROVER_ADDED",
                    "Added as approver: " + changeRequest.getTitle(),
                    "You have been added as an approver via group.",
                    "/change-requests/" + changeRequestId);
            emailService.sendApprovalRequestEmail(a.getUser().getEmail(), a.getUser().getFullName(),
                    changeRequest.getTitle(), changeRequestId.toString());
        }
        Set<UUID> savedIds = saved.stream().map(CrApproverEntity::getId).collect(java.util.stream.Collectors.toSet());
        return crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId).stream()
                .filter(a -> savedIds.contains(a.getId()))
                .toList();
    }

    public void removeApprover(UUID changeRequestId, UUID approverId, UUID actorUserId, String actorRole) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        assertNotCompleted(changeRequest);
        assertApproverManagementAllowed(changeRequest);

        CrApproverEntity approver = crApproverRepository.findById(approverId)
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "Approver not found."));
        if (!approver.getChangeRequest().getId().equals(changeRequestId)) {
            throw new DomainNotPermittedException(ERROR_NOT_FOUND, "Approver not found on this change request.");
        }
        if (approver.getStatus() != ApproverStatus.PENDING) {
            throw new InvalidStateTransitionException("APPROVER_DECISION_LOCKED",
                    "Approvers who already voted cannot be removed.");
        }

        changeRequest.getApprovers().remove(approver);
        crApproverRepository.delete(approver);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVER_REMOVED",
                Map.of(PAYLOAD_APPROVER_ID, approverId.toString()));
        auditLogService.log("CR_APPROVER_REMOVED", ENTITY_CHANGE_REQUEST, changeRequestId,
                actorUserId, resolveActorEmail(actorUserId),
                Map.of(PAYLOAD_APPROVER_ID, approverId.toString()), RequestContext.getCurrentIp());
        resequenceApprovers(changeRequestId);
    }

    public List<CrApproverEntity> reorderApprovers(UUID changeRequestId,
            List<UUID> approverIds,
            UUID actorUserId,
            String actorRole) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        assertNotCompleted(changeRequest);
        assertApproverManagementAllowed(changeRequest);

        List<CrApproverEntity> existing = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        if (existing.size() != approverIds.size()) {
            throw new InvalidRequestException("INVALID_ORDER",
                    "Approver order payload does not match approver set.");
        }

        for (int i = 0; i < approverIds.size(); i++) {
            UUID approverId = approverIds.get(i);
            CrApproverEntity approver = existing.stream()
                    .filter(a -> a.getId().equals(approverId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException("INVALID_ORDER",
                            "Approver order contains unknown IDs."));
            approver.setPosition(i + 1);
        }

        List<CrApproverEntity> saved = crApproverRepository.saveAll(existing).stream()
                .sorted(Comparator.comparingInt(CrApproverEntity::getPosition))
                .toList();
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVERS_REORDERED",
                Map.of(PAYLOAD_COUNT, saved.size()));
        auditLogService.log("CR_APPROVERS_REORDERED", ENTITY_CHANGE_REQUEST, changeRequestId,
                actorUserId, resolveActorEmail(actorUserId),
                Map.of(PAYLOAD_COUNT, saved.size()), RequestContext.getCurrentIp());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ApproverCandidate> searchApproverCandidates(String query, int limit) {
        String trimmed = query == null ? "" : query.trim();
        int cappedLimit = Math.max(1, Math.min(limit, 25));

        List<UserEntity> userMatches;
        List<io.audita.infrastructure.persistence.entity.GroupEntity> groupMatches;
        if (trimmed.isEmpty()) {
            userMatches = userRepository.findByStatus(UserStatus.ACTIVE, PageRequest.of(0, cappedLimit)).getContent();
            groupMatches = groupRepository.findAll(PageRequest.of(0, cappedLimit)).getContent();
        } else {
            userMatches = userRepository
                    .findByStatusAndFullNameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCaseOrderByFullNameAsc(
                            UserStatus.ACTIVE,
                            trimmed,
                            UserStatus.ACTIVE,
                            trimmed)
                    .stream()
                    .limit(cappedLimit)
                    .toList();
            groupMatches = groupRepository.findByNameContainingIgnoreCase(trimmed, PageRequest.of(0, cappedLimit))
                    .getContent();
        }

        List<ApproverCandidate> candidates = new java.util.ArrayList<>();
        userMatches.forEach(user -> {
            Set<String> roleNames = user.getRoles().stream()
                    .map(io.audita.infrastructure.persistence.entity.RoleEntity::getName)
                    .collect(Collectors.toSet());
            if (roleNames.stream().anyMatch(r -> "AUDITOR".equalsIgnoreCase(r))) {
                return;
            }
            String displayRole = roleNames.stream().findFirst().orElse(null);
            candidates.add(new ApproverCandidate(
                    user.getId(),
                    "USER",
                    user.getFullName(),
                    user.getEmail(),
                    displayRole));
        });
        groupMatches.forEach(group -> candidates.add(new ApproverCandidate(
                group.getId(),
                "GROUP",
                group.getName(),
                group.getDescription(),
                null)));
        return candidates;
    }

    public ChangeRequestEntity approve(UUID changeRequestId, UUID actorUserId) {
        log.info("CR approve: id={}, approver={}", changeRequestId, actorUserId);
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        ensurePendingApproval(changeRequest);

        CrApproverEntity approver = crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, actorUserId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER",
                        "You are not an approver on this change request."));
        UserEntity actor = approver.getUser();
        assertCreatorSelfApprovalRule(changeRequest, actorUserId, actor);

        if (changeRequest.getApprovalType() == ApprovalType.LINEAR) {
            CrApproverEntity next = nextPendingApprover(changeRequestId);
            if (next != null && !next.getId().equals(approver.getId())) {
                throw new DomainNotPermittedException("OUT_OF_SEQUENCE",
                        "Only the next pending approver can act in LINEAR mode.");
            }
        }

        approver.approve();
        changeRequest.markApprovalLocked();
        changeRequest.evaluateApprovalClosure();
        crApproverRepository.save(approver);
        ChangeRequestEntity updated = changeRequestRepository.save(changeRequest);
        logActivity(updated, actor, "CR_APPROVED",
                Map.of(PAYLOAD_APPROVER_ID, approver.getId().toString(), PAYLOAD_STATUS, updated.getStatus().name()));
        auditLogService.log("CR_APPROVED", ENTITY_CHANGE_REQUEST, updated.getId(),
                actorUserId, actor.getEmail(),
                Map.of(PAYLOAD_APPROVER_ID, approver.getId().toString(), PAYLOAD_STATUS, updated.getStatus().name()),
                RequestContext.getCurrentIp());
        if (updated.getCreatedBy() != null) {
            notificationService.createAndPush(updated.getCreatedBy().getId(), "APPROVAL_DECIDED",
                    actor.getFullName() + " approved " + updated.getTitle(),
                    "The change request has been approved.",
                    "/change-requests/" + updated.getId());
            emailService.sendApprovalDecisionEmail(updated.getCreatedBy().getEmail(),
                    updated.getCreatedBy().getFullName(), updated.getTitle(),
                    updated.getId().toString(), "approved", actor.getFullName());
        }
        initializeCreator(updated);
        return updated;
    }

    public ChangeRequestEntity reject(UUID changeRequestId, UUID actorUserId, String reason) {
        log.info("CR reject: id={}, approver={}", changeRequestId, actorUserId);
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        ensurePendingApproval(changeRequest);

        CrApproverEntity approver = crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, actorUserId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER",
                        "You are not an approver on this change request."));
        UserEntity actor = approver.getUser();
        assertCreatorSelfApprovalRule(changeRequest, actorUserId, actor);

        if (changeRequest.getApprovalType() == ApprovalType.LINEAR) {
            CrApproverEntity next = nextPendingApprover(changeRequestId);
            if (next != null && !next.getId().equals(approver.getId())) {
                throw new DomainNotPermittedException("OUT_OF_SEQUENCE",
                        "Only the next pending approver can act in LINEAR mode.");
            }
        }

        approver.reject(reason);
        changeRequest.markApprovalLocked();
        changeRequest.evaluateApprovalClosure();
        crApproverRepository.save(approver);
        ChangeRequestEntity updated = changeRequestRepository.save(changeRequest);
        logActivity(updated, actor, "CR_REJECTED", Map.of(PAYLOAD_APPROVER_ID, approver.getId().toString(), "reason",
                reason, PAYLOAD_STATUS, updated.getStatus().name()));
        auditLogService.log("CR_REJECTED", ENTITY_CHANGE_REQUEST, updated.getId(),
                actorUserId, actor.getEmail(),
                Map.of(PAYLOAD_APPROVER_ID, approver.getId().toString(), PAYLOAD_STATUS, updated.getStatus().name(), "reason", reason),
                RequestContext.getCurrentIp());
        if (updated.getCreatedBy() != null) {
            notificationService.createAndPush(updated.getCreatedBy().getId(), "APPROVAL_DECIDED",
                    actor.getFullName() + " rejected " + updated.getTitle(),
                    "The change request has been rejected. Reason: " + (reason != null ? reason : "No reason provided"),
                    "/change-requests/" + updated.getId());
            emailService.sendApprovalDecisionEmail(updated.getCreatedBy().getEmail(),
                    updated.getCreatedBy().getFullName(), updated.getTitle(),
                    updated.getId().toString(), "rejected", actor.getFullName());
        }
        initializeCreator(updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<ActivityStreamEntity> listActivity(UUID changeRequestId) {
        getById(changeRequestId);
        List<ActivityStreamEntity> activityEntries = activityStreamRepository
                .findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
        activityEntries.forEach(this::initializeActivityActor);
        return activityEntries;
    }

    @Transactional(readOnly = true)
    public List<AttachmentEntity> listAttachments(UUID changeRequestId) {
        getById(changeRequestId);
        List<AttachmentEntity> attachments = attachmentRepository
                .findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
        attachments.forEach(this::initializeAttachmentUploader);
        return attachments;
    }

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(UUID changeRequestId, UUID attachmentId) {
        // Confirm the CR exists (applies draft-visibility check via getById).
        getById(changeRequestId);
        AttachmentEntity attachment = attachmentRepository
                .findByIdAndChangeRequestId(attachmentId, changeRequestId)
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "Attachment not found."));

        Path filePath = Path.of(attachment.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new DomainNotPermittedException("FILE_MISSING", "Attachment file is no longer available.");
        }

        try {
            return new AttachmentDownload(
                    Files.newInputStream(filePath),
                    attachment.getFileName(),
                    attachment.getMimeType() != null ? attachment.getMimeType() : "application/octet-stream",
                    attachment.getSizeBytes());
        } catch (IOException _) {
            throw new DomainNotPermittedException("DOWNLOAD_FAILED", "Could not read attachment file.");
        }
    }

    public AttachmentEntity uploadAttachment(UUID changeRequestId,
            UUID uploaderId,
            String uploaderRole,
            MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainNotPermittedException("EMPTY_FILE", "Attachment file is required.");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new DomainNotPermittedException("FILE_TOO_LARGE", "Attachment exceeds maximum allowed size.");
        }

        ChangeRequestEntity changeRequest = getById(changeRequestId);
        assertCanMutate(changeRequest, uploaderId, uploaderRole);
        assertNotCompleted(changeRequest);

        String mimeType = file.getContentType();
        if (!isAllowedMimeType(mimeType)) {
            throw new DomainNotPermittedException("INVALID_FILE_TYPE", "Attachment file type is not allowed.");
        }
        String originalName = file.getOriginalFilename();
        if (!isExtensionAllowed(originalName, mimeType)) {
            throw new DomainNotPermittedException("INVALID_FILE_TYPE",
                    "File extension does not match the declared content type.");
        }
        if (!isSignatureValid(file, mimeType)) {
            throw new DomainNotPermittedException("INVALID_FILE_CONTENT",
                    "Attachment content does not match file type.");
        }

        UserEntity uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "Uploader not found."));

        String tenant = TenantContext.getCurrentTenant();
        String safeOriginalName = file.getOriginalFilename() == null ? "attachment.bin" : file.getOriginalFilename();
        // UUID prefix prevents collisions; normalizeFileName produces a lowercase,
        // hyphenated, filesystem-safe name.
        String storedName = UUID.randomUUID() + "-" + normalizeFileName(safeOriginalName);

        Path storageDir = Path.of(storageBasePath, tenant == null ? "public" : tenant, changeRequestId.toString())
                .toAbsolutePath().normalize();
        Path outputPath = storageDir.resolve(storedName).normalize();
        // Defense-in-depth: confirm the resolved path is still inside the storage
        // directory.
        if (!outputPath.startsWith(storageDir)) {
            throw new DomainNotPermittedException("INVALID_FILE_PATH", "Invalid file path.");
        }

        try {
            Files.createDirectories(storageDir);
            Files.copy(file.getInputStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage());
            throw new DomainNotPermittedException("UPLOAD_FAILED", "Could not persist uploaded file.");
        }

        AttachmentEntity attachment = new AttachmentEntity(
                changeRequest,
                uploader,
                safeOriginalName,
                mimeType,
                file.getSize(),
                outputPath.toString());
        AttachmentEntity saved = attachmentRepository.save(attachment);
        logActivity(changeRequest, uploader, "CR_ATTACHMENT_UPLOADED", Map.of(
                "attachmentId", saved.getId().toString(),
                "fileName", safeOriginalName));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestCustomFieldEntity> listCustomFields(UUID changeRequestId) {
        getById(changeRequestId);
        return customFieldRepository.findByIdChangeRequestId(changeRequestId);
    }

    public List<ChangeRequestCustomFieldEntity> upsertCustomFields(UUID changeRequestId,
            List<FieldValue> fields,
            UUID actorUserId,
            String actorRole) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        assertNotCompleted(changeRequest);

        for (FieldValue fv : fields) {
            CustomFieldDefinitionEntity def = customFieldDefinitionRepository.findById(fv.fieldId())
                    .orElseThrow(() -> new NotFoundException("Custom field definition", fv.fieldId()));

            String raw = fv.value();
            boolean blank = raw == null || raw.isBlank();

            if (def.isRequired() && blank) {
                throw new InvalidRequestException("INVALID_INPUT",
                        "'" + def.getLabel() + "' is required.");
            }

            if (blank) {
                continue;
            }

            if ("NUMBER".equalsIgnoreCase(def.getFieldType())) {
                validateNumberValue(def, raw);
            }
        }

        customFieldRepository.deleteByIdChangeRequestId(changeRequestId);

        List<ChangeRequestCustomFieldEntity> newRows = fields.stream()
                .map(f -> new ChangeRequestCustomFieldEntity(changeRequest, f.fieldId(), f.value()))
                .toList();
        List<ChangeRequestCustomFieldEntity> saved = customFieldRepository.saveAll(newRows);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(PAYLOAD_COUNT, saved.size());
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_CUSTOM_FIELDS_UPDATED", payload);
        return saved;
    }

    private void validateNumberValue(CustomFieldDefinitionEntity def, String raw) {
        BigDecimal value;
        try {
            value = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("INVALID_INPUT",
                    "Value for '" + def.getLabel() + "' must be a valid number.");
        }

        if (value.scale() > 2) {
            throw new InvalidRequestException("INVALID_INPUT",
                    "Value for '" + def.getLabel() + "' must have at most 2 decimal places.");
        }

        if (def.getMinValue() != null && value.compareTo(def.getMinValue()) < 0) {
            throw new InvalidRequestException("INVALID_INPUT",
                    "Value for '" + def.getLabel() + "' must be at least " + def.getMinValue().stripTrailingZeros().toPlainString() + ".");
        }

        if (def.getMaxValue() != null && value.compareTo(def.getMaxValue()) > 0) {
            throw new InvalidRequestException("INVALID_INPUT",
                    "Value for '" + def.getLabel() + "' must be at most " + def.getMaxValue().stripTrailingZeros().toPlainString() + ".");
        }
    }

    public record FieldValue(UUID fieldId, String value) {
    }

    public record CreateRequest(String title,
            String description,
            Priority priority,
            RiskLevel riskLevel,
            String category,
            ApprovalType approvalType,
            OffsetDateTime scheduledStart,
            OffsetDateTime scheduledEnd,
            List<String> affectedSystems,
            UUID createdById,
            RequestWorkflowMode workflowMode,
            UUID requestDepartmentId,
            UUID destinationDepartmentId,
            UUID requestGroupId,
            UUID destinationGroupId) {
    }

    public record UpdateRequest(UUID id,
            String title,
            String description,
            Priority priority,
            RiskLevel riskLevel,
            String category,
            ApprovalType approvalType,
            OffsetDateTime scheduledStart,
            OffsetDateTime scheduledEnd,
            List<String> affectedSystems,
            UUID actorUserId,
            String actorRole,
            RequestWorkflowMode workflowMode,
            UUID requestDepartmentId,
            UUID destinationDepartmentId,
            UUID requestGroupId,
            UUID destinationGroupId) {
    }

    private void ensurePendingApproval(ChangeRequestEntity changeRequest) {
        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING_APPROVAL) {
            throw new InvalidStateTransitionException(
                    "Approver actions are allowed only in PENDING_APPROVAL state.");
        }
    }

    private CrApproverEntity nextPendingApprover(UUID changeRequestId) {
        return crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId).stream()
                .filter(a -> a.getStatus() == ApproverStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    private void resequenceApprovers(UUID changeRequestId) {
        List<CrApproverEntity> approvers = crApproverRepository
                .findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        for (int i = 0; i < approvers.size(); i++) {
            approvers.get(i).setPosition(i + 1);
        }
        crApproverRepository.saveAll(approvers);
    }

    private void ensureDefaultApprovers(ChangeRequestEntity changeRequest) {
        UUID changeRequestId = changeRequest.getId();
        List<UUID> configuredUserIds = resolveUuidListSetting(WORKFLOW_DEFAULT_APPROVER_USER_IDS_KEY);
        List<UUID> configuredGroupIds = resolveUuidListSetting(WORKFLOW_DEFAULT_APPROVER_GROUP_IDS_KEY);

        if (configuredUserIds.isEmpty() && configuredGroupIds.isEmpty()) {
            return;
        }

        List<CrApproverEntity> existingApprovers = crApproverRepository
                .findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        Map<UUID, CrApproverEntity> existingByUserId = existingApprovers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        approver -> approver.getUser().getId(),
                        approver -> approver,
                        (left, ignored) -> left,
                        LinkedHashMap::new));

        List<UserEntity> usersFromSettings = configuredUserIds.isEmpty()
                ? List.of()
                : userRepository.findByIdInAndStatusOrderByFullNameAsc(configuredUserIds, UserStatus.ACTIVE);
        List<UserEntity> usersFromGroups = configuredGroupIds.isEmpty()
                ? List.of()
                : groupMemberRepository.findDistinctUsersByGroupIdInAndUserStatus(configuredGroupIds,
                        UserStatus.ACTIVE);

        Map<UUID, UserEntity> eligibleUsersById = new LinkedHashMap<>();
        usersFromSettings.forEach(user -> eligibleUsersById.put(user.getId(), user));
        usersFromGroups.forEach(user -> eligibleUsersById.put(user.getId(), user));
        List<UserEntity> eligibleUsers = eligibleUsersById.values().stream()
                .sorted(java.util.Comparator.comparing(UserEntity::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int nextPosition = existingApprovers.size() + 1;
        List<CrApproverEntity> newApprovers = new java.util.ArrayList<>();
        for (UserEntity user : eligibleUsers) {
            if (existingByUserId.containsKey(user.getId())) {
                continue;
            }

            CrApproverEntity approver = new CrApproverEntity(
                    changeRequest,
                    user,
                    nextPosition++);
            newApprovers.add(approver);
        }

        if (newApprovers.isEmpty()) {
            return;
        }

        crApproverRepository.saveAll(newApprovers);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVERS_AUTO_ADDED", Map.of(
                PAYLOAD_COUNT, newApprovers.size(),
                PAYLOAD_STATUS, changeRequest.getStatus().name()));
    }

    private List<UUID> resolveUuidListSetting(String key) {
        return orgSettingRepository.findById(key)
                .map(setting -> setting.getValue() == null ? "" : setting.getValue())
                .stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(UUID::fromString)
                .distinct()
                .toList();
    }

    public record ApproverCandidate(UUID id, String kind, String label, String secondary, String role) {
    }

    private void logActivity(ChangeRequestEntity changeRequest,
            UserEntity actor,
            String actionType,
            Map<String, Object> payload) {
        activityStreamRepository.save(new ActivityStreamEntity(changeRequest, actor, actionType, payload));
    }

    private void initializeCreator(ChangeRequestEntity changeRequest) {
        if (changeRequest.getCreatedBy() != null) {
            changeRequest.getCreatedBy().getEmail();
        }
    }

    private void initializeActivityActor(ActivityStreamEntity activityStream) {
        UserEntity actor = activityStream.getActor();
        if (actor != null) {
            actor.getEmail();
            actor.getFullName();
        }
    }

    private void initializeAttachmentUploader(AttachmentEntity attachment) {
        UserEntity uploader = attachment.getUploader();
        if (uploader != null) {
            uploader.getId();
            uploader.getFullName();
        }
    }

    private void assertNotCompleted(ChangeRequestEntity changeRequest) {
        if (changeRequest.getCompletionStatus() == CompletionStatus.COMPLETED) {
            throw new InvalidStateTransitionException("REQUEST_COMPLETED",
                    "This request is completed and read-only.");
        }
    }

    private void assertCanMutate(ChangeRequestEntity changeRequest, UUID actorUserId, Set<String> actorPermissions) {
        if (actorPermissions != null && actorPermissions.contains("cr.view.all")) {
            return;
        }
        UserEntity createdBy = changeRequest.getCreatedBy();
        if (createdBy == null || !createdBy.getId().equals(actorUserId)) {
            log.warn("Permission denied: actor {} cannot mutate CR {} (owner={})",
                    actorUserId, changeRequest.getId(),
                    createdBy != null ? createdBy.getId() : null);
            throw new DomainNotPermittedException("FORBIDDEN", "You are not allowed to modify this change request.");
        }
    }

    private void assertCanMutate(ChangeRequestEntity changeRequest, UUID actorUserId, String actorRole) {
        Set<String> perms = new HashSet<>();
        if (actorRole != null && ELEVATED_ROLES.contains(actorRole.toUpperCase())) {
            perms.add("cr.view.all");
        }
        assertCanMutate(changeRequest, actorUserId, perms);
    }

    private void assertCreatorSelfApprovalRule(ChangeRequestEntity changeRequest, UUID actorUserId, UserEntity actor) {
        UserEntity createdBy = changeRequest.getCreatedBy();
        if (createdBy == null || !createdBy.getId().equals(actorUserId)) {
            return;
        }
        if (isElevatedActor(actor)) {
            return;
        }
        throw new DomainNotPermittedException("REQUESTER_SELF_APPROVAL_FORBIDDEN",
                "The request creator cannot approve or reject their own change request.");
    }

    private boolean isElevatedActor(UserEntity actor) {
        if (actor == null) {
            return false;
        }
        if (actor.getRole() != null && ELEVATED_ROLES.contains(actor.getRole().getName().toUpperCase())) {
            return true;
        }
        return actor.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().toUpperCase())
                .anyMatch(ELEVATED_ROLES::contains);
    }

    private boolean hasGlobalViewAccess(Set<String> viewerPermissions) {
        return viewerPermissions != null && viewerPermissions.contains("cr.view.all");
    }

    private boolean hasGlobalViewAccess(String viewerRole) {
        String normalizedRole = viewerRole == null ? "" : viewerRole.toUpperCase();
        return GLOBAL_VIEW_ROLES.contains(normalizedRole);
    }

    private void assertApproverManagementAllowed(ChangeRequestEntity changeRequest) {
        if (!changeRequest.getStatus().isEditable()) {
            throw new InvalidStateTransitionException("APPROVER_MUTATION_CLOSED",
                    "Approvers can only be modified while the change request is open.");
        }
    }

    private String resolveActorEmail(UUID actorUserId) {
        if (actorUserId == null) {
            return null;
        }
        return userRepository.findById(actorUserId)
                .map(UserEntity::getEmail)
                .orElse(null);
    }

    private boolean isViewerAllowed(ChangeRequestEntity changeRequest, UUID viewerId) {
        UserEntity createdBy = changeRequest.getCreatedBy();
        if (createdBy != null && createdBy.getId().equals(viewerId)) {
            return true;
        }
        if (crApproverRepository.findByChangeRequestIdAndUserId(changeRequest.getId(), viewerId).isPresent()) {
            return true;
        }
        if (crWatcherRepository.existsByChangeRequestIdAndUserId(changeRequest.getId(), viewerId)) {
            return true;
        }
        return deploymentService.getByRequestId(changeRequest.getId())
                .map(d -> d.getAssignee() != null && d.getAssignee().getId().equals(viewerId))
                .orElse(false);
    }

    private boolean isAuditor(UserEntity user) {
        return user.getRoles().stream().anyMatch(r -> "AUDITOR".equalsIgnoreCase(r.getName()));
    }

    private boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        return java.util.Arrays.stream(allowedMimeTypes.split(","))
                .map(String::trim)
                .anyMatch(allowed -> allowed.equalsIgnoreCase(mimeType));
    }

    /**
     * Cross-checks the file's extension against its declared MIME type.
     * Prevents spoofing a DOCX as a PDF, or embedding a script with a .png
     * extension.
     */
    private boolean isExtensionAllowed(String fileName, String mimeType) {
        if (fileName == null || mimeType == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> lower .endsWith(".png");
            case "image/jpeg" -> lower .endsWith(".jpg") || lower.endsWith(".jpeg");
            case "application/pdf" -> lower .endsWith(".pdf");
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> lower .endsWith(".docx");
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> lower .endsWith(".xlsx");
            default -> false ;
        };
    }

    private boolean isSignatureValid(MultipartFile file, String mimeType) {
        if (mimeType == null) {
            return false;
        }
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(8);
            if (mimeType.equalsIgnoreCase("application/pdf")) {
                // %PDF
                return startsWith(header, new byte[] { 0x25, 0x50, 0x44, 0x46 });
            }
            if (mimeType.equalsIgnoreCase("image/png")) {
                // \x89PNG
                return startsWith(header, new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });
            }
            if (mimeType.equalsIgnoreCase("image/jpeg")) {
                // FF D8 FF
                return startsWith(header, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
            }
            // DOCX and XLSX are Office Open XML — both are ZIP archives (PK\x03\x04).
            // Extension + MIME type validation (above) distinguishes them from plain ZIPs.
            if (mimeType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    || mimeType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                return startsWith(header, new byte[] { 0x50, 0x4B, 0x03, 0x04 });
            }
            return false;
        } catch (IOException _) {
            return false;
        }
    }

    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Produces a filesystem-safe, lowercase filename for on-disk storage.
     * The original name is preserved separately for display and download.
     * Rules: stem lowercased; any run of non-alphanumeric chars → single hyphen;
     * leading/trailing hyphens stripped; extension lowercased and preserved.
     * Example: "Network Diagram - Acme Inc.DOCX" → "network-diagram-acme-inc.docx"
     */
    private String normalizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "attachment";
        }
        int dotIndex = originalName.lastIndexOf('.');
        String stem = dotIndex > 0 ? originalName.substring(0, dotIndex) : originalName;
        String ext = dotIndex > 0 ? originalName.substring(dotIndex).toLowerCase() : "";
        String normalizedStem = normalizeFileStem(stem);
        if (normalizedStem.isEmpty()) {
            normalizedStem = "attachment";
        }
        return normalizedStem + ext;
    }

    private String normalizeFileStem(String stem) {
        StringBuilder normalized = new StringBuilder(stem.length());
        boolean previousWasHyphen = false;

        for (int index = 0; index < stem.length(); index++) {
            char character = Character.toLowerCase(stem.charAt(index));
            if (character >= 'a' && character <= 'z' || character >= '0' && character <= '9') {
                normalized.append(character);
                previousWasHyphen = false;
            } else if (!previousWasHyphen && !normalized.isEmpty()) {
                normalized.append('-');
                previousWasHyphen = true;
            }
        }

        int length = normalized.length();
        if (length > 0 && normalized.charAt(length - 1) == '-') {
            normalized.setLength(length - 1);
        }

        return normalized.toString();
    }

    private String[] normalizeAffectedSystems(List<String> affectedSystems) {
        if (affectedSystems == null || affectedSystems.isEmpty()) {
            return new String[0];
        }
        return affectedSystems.stream()
                .filter(s -> s != null && !s.isBlank())
                .toArray(String[]::new);
    }

    private String sanitizeRichText(String html) {
        return htmlSanitizer.sanitize(html);
    }

    private String generateDisplayId() {
        String prefix = orgSettingRepository.findById(REQUEST_ID_PREFIX_KEY)
                .map(OrgSettingEntity::getValue)
                .orElse("RQ");
        long sequence = orgSettingRepository.findById(REQUEST_ID_SEQUENCE_KEY)
                .map(s -> {
                    String v = s.getValue();
                    if (v == null || v.isBlank()) return 0L;
                    try { return Long.parseLong(v.trim()); }
                    catch (NumberFormatException e) { return 0L; }
                })
                .orElse(0L);
        long next = sequence + 1;
        orgSettingRepository.save(new OrgSettingEntity(REQUEST_ID_SEQUENCE_KEY, Long.toString(next)));
        return prefix + "-" + String.format("%06d", next);
    }

    private long resolveSlaHours(Priority priority) {
        if (priority == null) {
            return 48;
        }
        String key = switch (priority) {
            case CRITICAL -> "sla.deadline_hours.critical" ;
            case HIGH -> "sla.deadline_hours.high" ;
            case MEDIUM -> "sla.deadline_hours.medium" ;
            case LOW -> "sla.deadline_hours.low" ;
        };
        long defaultHours = switch (priority) {
            case CRITICAL -> 8 ;
            case HIGH -> 24 ;
            case MEDIUM -> 48 ;
            case LOW -> 72 ;
        };
        return orgSettingRepository.findById(key)
                .map(setting -> setting.getValue().trim())
                .map(value -> {
                    try {
                        long parsed = Long.parseLong(value);
                        return parsed > 0 ? parsed : defaultHours;
                    } catch (NumberFormatException _) {
                        return defaultHours;
                    }
                })
                .orElse(defaultHours);
    }
}
