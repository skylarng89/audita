package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.AttachmentEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.AttachmentRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestCustomFieldRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ChangeRequestService {

    private static final Set<String> ELEVATED_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final List<String> AUTO_APPROVER_ROLE_NAMES = List.of("Approver", "Auditor");
    private static final String ERROR_NOT_FOUND = "NOT_FOUND";
    private static final String PAYLOAD_STATUS = "status";
    private static final String PAYLOAD_COUNT = "count";
    private static final String ENTITY_CHANGE_REQUEST = "change_request";
    private static final String PAYLOAD_APPROVER_ID = "approverId";

    private final ChangeRequestRepository changeRequestRepository;
    private final CrApproverRepository crApproverRepository;
    private final ChangeRequestCustomFieldRepository customFieldRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final OrgSettingRepository orgSettingRepository;
    private final AuditLogService auditLogService;

    @Value("${audita.storage.local.base-path:/data/uploads}")
    private String storageBasePath;

    @Value("${audita.upload.max-size-bytes:10485760}")
    private long maxUploadSizeBytes;

    @Value("${audita.upload.allowed-mime-types:application/pdf,image/png,image/jpeg,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}")
    private String allowedMimeTypes;

    public ChangeRequestService(ChangeRequestRepository changeRequestRepository,
            CrApproverRepository crApproverRepository,
            ChangeRequestCustomFieldRepository customFieldRepository,
            ActivityStreamRepository activityStreamRepository,
            AttachmentRepository attachmentRepository,
            UserRepository userRepository,
            OrgSettingRepository orgSettingRepository,
            AuditLogService auditLogService) {
        this.changeRequestRepository = changeRequestRepository;
        this.crApproverRepository = crApproverRepository;
        this.customFieldRepository = customFieldRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.orgSettingRepository = orgSettingRepository;
        this.auditLogService = auditLogService;
    }

    public ChangeRequestEntity create(CreateRequest request) {
        UserEntity createdBy = userRepository.findById(request.createdById())
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "User not found."));

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setTitle(request.title());
        changeRequest.setDescription(request.description());
        changeRequest.setPriority(request.priority());
        changeRequest.setRiskLevel(request.riskLevel());
        changeRequest.setCategory(request.category());
        changeRequest.setApprovalType(
                request.approvalType() != null ? request.approvalType() : ApprovalType.NON_LINEAR);
        changeRequest.setScheduledStart(request.scheduledStart());
        changeRequest.setScheduledEnd(request.scheduledEnd());
        changeRequest.setCreatedBy(createdBy);
        changeRequest.setAffectedSystems(normalizeAffectedSystems(request.affectedSystems()));
        ChangeRequestEntity created = changeRequestRepository.save(changeRequest);
        logActivity(created, createdBy, "CR_CREATED", Map.of(PAYLOAD_STATUS, created.getStatus().name()));
        auditLogService.log("CR_CREATED", ENTITY_CHANGE_REQUEST, created.getId(),
                createdBy.getId(), createdBy.getEmail(),
                Map.of("title", created.getTitle(), "priority", created.getPriority().name()), null);
        initializeCreator(created);
        return created;
    }

    public ChangeRequestEntity update(UpdateRequest request) {
        ChangeRequestEntity current = getById(request.id());
        assertCanMutate(current, request.actorUserId(), request.actorRole());
        if (current.getStatus().isClosed()) {
            throw new InvalidStateTransitionException("Closed change requests cannot be edited.");
        }

        if (request.title() != null) {
            current.setTitle(request.title());
        }
        if (request.description() != null) {
            current.setDescription(request.description());
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

        ChangeRequestEntity updated = changeRequestRepository.save(current);
        logActivity(updated, updated.getCreatedBy(), "CR_UPDATED", Map.of(PAYLOAD_STATUS, updated.getStatus().name()));
        initializeCreator(updated);
        return updated;
    }

    public ChangeRequestEntity submit(UUID id, UUID actorUserId, String actorRole) {
        ChangeRequestEntity changeRequest = getById(id);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        ensureDefaultApproversForSubmission(changeRequest);
        changeRequest.submit();
        changeRequest.setSlaDeadline(OffsetDateTime.now().plusHours(resolveSlaHours(changeRequest.getPriority())));
        ChangeRequestEntity submitted = changeRequestRepository.save(changeRequest);
        logActivity(submitted, submitted.getCreatedBy(), "CR_SUBMITTED",
                Map.of(PAYLOAD_STATUS, submitted.getStatus().name()));
        auditLogService.log("CR_SUBMITTED", ENTITY_CHANGE_REQUEST, submitted.getId(),
                actorUserId, submitted.getCreatedBy() != null ? submitted.getCreatedBy().getEmail() : null,
                Map.of(PAYLOAD_STATUS, "PENDING_APPROVAL"), null);
        initializeCreator(submitted);
        return submitted;
    }

    public void cancel(UUID id, UUID actorUserId, String actorRole) {
        ChangeRequestEntity changeRequest = getById(id);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        changeRequest.cancel();
        ChangeRequestEntity cancelled = changeRequestRepository.save(changeRequest);
        logActivity(cancelled, cancelled.getCreatedBy(), "CR_CANCELLED",
                Map.of(PAYLOAD_STATUS, cancelled.getStatus().name()));
        auditLogService.log("CR_CANCELLED", ENTITY_CHANGE_REQUEST, cancelled.getId(),
                actorUserId, cancelled.getCreatedBy() != null ? cancelled.getCreatedBy().getEmail() : null,
                Map.of(PAYLOAD_STATUS, "CANCELLED"), null);
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequestEntity> list(ChangeRequestStatus status,
            Priority priority,
            String category,
            UUID createdBy,
            UUID viewerId,
            Pageable pageable) {
        return changeRequestRepository.findAllFiltered(
                status, priority, category, createdBy, viewerId, ChangeRequestStatus.DRAFT, pageable)
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
    public ChangeRequestEntity getById(UUID id, UUID viewerId) {
        ChangeRequestEntity changeRequest = getById(id);
        // Draft CRs are private to their creator. Return NOT_FOUND (not 403) to
        // avoid leaking that the resource exists to users who cannot see it.
        if (changeRequest.getStatus() == ChangeRequestStatus.DRAFT &&
                !changeRequest.getCreatedBy().getId().equals(viewerId)) {
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

    public CrApproverEntity addApprover(UUID changeRequestId, UUID userId, boolean isRequired) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        if (changeRequest.isApprovalLocked()) {
            throw new InvalidStateTransitionException("Approvers are locked and cannot be changed.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "User not found."));

        if (crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, userId).isPresent()) {
            throw new DomainNotPermittedException("DUPLICATE_APPROVER", "User is already an approver on this CR.");
        }

        int position = crApproverRepository.countByChangeRequestId(changeRequestId) + 1;
        CrApproverEntity approver = new CrApproverEntity(changeRequest, user, isRequired, position, true);
        CrApproverEntity created = crApproverRepository.save(approver);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVER_ADDED",
                Map.of(PAYLOAD_APPROVER_ID, created.getId().toString(), "userId", userId.toString()));
        return created;
    }

    public void removeApprover(UUID changeRequestId, UUID approverId, UUID actorUserId, String actorRole) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        if (changeRequest.isApprovalLocked()) {
            throw new InvalidStateTransitionException("Approvers are locked and cannot be changed.");
        }

        CrApproverEntity approver = crApproverRepository.findById(approverId)
                .orElseThrow(() -> new DomainNotPermittedException(ERROR_NOT_FOUND, "Approver not found."));
        if (!approver.getChangeRequest().getId().equals(changeRequestId)) {
            throw new DomainNotPermittedException(ERROR_NOT_FOUND, "Approver not found on this change request.");
        }

        crApproverRepository.delete(approver);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVER_REMOVED",
                Map.of(PAYLOAD_APPROVER_ID, approverId.toString()));
        resequenceApprovers(changeRequestId);
    }

    public List<CrApproverEntity> reorderApprovers(UUID changeRequestId, List<UUID> approverIds) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        if (changeRequest.isApprovalLocked()) {
            throw new InvalidStateTransitionException("Approvers are locked and cannot be changed.");
        }

        List<CrApproverEntity> existing = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        if (existing.size() != approverIds.size()) {
            throw new DomainNotPermittedException("INVALID_ORDER",
                    "Approver order payload does not match approver set.");
        }

        for (int i = 0; i < approverIds.size(); i++) {
            UUID approverId = approverIds.get(i);
            CrApproverEntity approver = existing.stream()
                    .filter(a -> a.getId().equals(approverId))
                    .findFirst()
                    .orElseThrow(() -> new DomainNotPermittedException("INVALID_ORDER",
                            "Approver order contains unknown IDs."));
            approver.setPosition(i + 1);
        }

        List<CrApproverEntity> saved = crApproverRepository.saveAll(existing).stream()
                .sorted(Comparator.comparingInt(CrApproverEntity::getPosition))
                .toList();
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVERS_REORDERED",
                Map.of(PAYLOAD_COUNT, saved.size()));
        return saved;
    }

    public ChangeRequestEntity approve(UUID changeRequestId, UUID actorUserId) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        ensurePendingApproval(changeRequest);

        CrApproverEntity approver = crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, actorUserId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER",
                        "You are not an approver on this change request."));
        UserEntity actor = approver.getUser();

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
                null);
        initializeCreator(updated);
        return updated;
    }

    public ChangeRequestEntity reject(UUID changeRequestId, UUID actorUserId, String reason) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        ensurePendingApproval(changeRequest);

        CrApproverEntity approver = crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, actorUserId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER",
                        "You are not an approver on this change request."));
        UserEntity actor = approver.getUser();

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
                Map.of(PAYLOAD_APPROVER_ID, approver.getId().toString(), PAYLOAD_STATUS, updated.getStatus().name()),
                null);
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
        } catch (IOException _) {
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
            UUID createdById) {
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
            String actorRole) {
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

    private void ensureDefaultApproversForSubmission(ChangeRequestEntity changeRequest) {
        UUID changeRequestId = changeRequest.getId();
        List<CrApproverEntity> existingApprovers = crApproverRepository
                .findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        Map<UUID, CrApproverEntity> existingByUserId = existingApprovers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        approver -> approver.getUser().getId(),
                        approver -> approver,
                        (left, ignored) -> left,
                        LinkedHashMap::new));

        List<UserEntity> eligibleUsers = userRepository
                .findByRole_NameInAndStatusOrderByFullNameAsc(AUTO_APPROVER_ROLE_NAMES, UserStatus.ACTIVE);

        int nextPosition = existingApprovers.size() + 1;
        List<CrApproverEntity> newApprovers = new java.util.ArrayList<>();
        for (UserEntity user : eligibleUsers) {
            if (existingByUserId.containsKey(user.getId())) {
                continue;
            }

            String roleName = user.getRole() != null ? user.getRole().getName() : "";
            boolean isRequired = "Approver".equalsIgnoreCase(roleName);
            CrApproverEntity approver = new CrApproverEntity(
                    changeRequest,
                    user,
                    isRequired,
                    nextPosition++,
                    false);
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

    private void assertCanMutate(ChangeRequestEntity changeRequest, UUID actorUserId, String actorRole) {
        String normalizedRole = actorRole == null ? "" : actorRole.toUpperCase();
        if (ELEVATED_ROLES.contains(normalizedRole)) {
            return;
        }
        UserEntity createdBy = changeRequest.getCreatedBy();
        if (createdBy == null || !createdBy.getId().equals(actorUserId)) {
            throw new DomainNotPermittedException("FORBIDDEN", "You are not allowed to modify this change request.");
        }
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