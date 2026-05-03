package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
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

    private final ChangeRequestRepository changeRequestRepository;
    private final CrApproverRepository crApproverRepository;
    private final ChangeRequestCustomFieldRepository customFieldRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;

    @Value("${audita.storage.local.base-path:/data/uploads}")
    private String storageBasePath;

    @Value("${audita.upload.max-size-bytes:10485760}")
    private long maxUploadSizeBytes;

    @Value("${audita.upload.allowed-mime-types:application/pdf,image/png,image/jpeg,text/plain,text/csv,application/json,application/zip}")
    private String allowedMimeTypes;

    public ChangeRequestService(ChangeRequestRepository changeRequestRepository,
                                CrApproverRepository crApproverRepository,
                                ChangeRequestCustomFieldRepository customFieldRepository,
                                ActivityStreamRepository activityStreamRepository,
                                AttachmentRepository attachmentRepository,
                                UserRepository userRepository) {
        this.changeRequestRepository = changeRequestRepository;
        this.crApproverRepository = crApproverRepository;
        this.customFieldRepository = customFieldRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
    }

    public ChangeRequestEntity create(String title,
                                      String description,
                                      Priority priority,
                                      RiskLevel riskLevel,
                                      String category,
                                      ApprovalType approvalType,
                                      OffsetDateTime scheduledStart,
                                      OffsetDateTime scheduledEnd,
                                      String[] affectedSystems,
                                      UUID createdById) {
        UserEntity createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "User not found."));

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setTitle(title);
        changeRequest.setDescription(description);
        changeRequest.setPriority(priority);
        changeRequest.setRiskLevel(riskLevel);
        changeRequest.setCategory(category);
        changeRequest.setApprovalType(approvalType);
        changeRequest.setScheduledStart(scheduledStart);
        changeRequest.setScheduledEnd(scheduledEnd);
        changeRequest.setCreatedBy(createdBy);
        changeRequest.setAffectedSystems(normalizeAffectedSystems(affectedSystems));
        ChangeRequestEntity created = changeRequestRepository.save(changeRequest);
        logActivity(created, createdBy, "CR_CREATED", Map.of("status", created.getStatus().name()));
        initializeCreator(created);
        return created;
    }

    public ChangeRequestEntity update(UUID id,
                                      String title,
                                      String description,
                                      Priority priority,
                                      RiskLevel riskLevel,
                                      String category,
                                      ApprovalType approvalType,
                                      OffsetDateTime scheduledStart,
                                      OffsetDateTime scheduledEnd,
                                      String[] affectedSystems,
                                      UUID actorUserId,
                                      String actorRole) {
        ChangeRequestEntity current = getById(id);
        assertCanMutate(current, actorUserId, actorRole);
        if (current.getStatus().isClosed()) {
            throw new InvalidStateTransitionException("Closed change requests cannot be edited.");
        }

        if (title != null) {
            current.setTitle(title);
        }
        if (description != null) {
            current.setDescription(description);
        }
        if (priority != null) {
            current.setPriority(priority);
        }
        if (riskLevel != null) {
            current.setRiskLevel(riskLevel);
        }
        if (category != null) {
            current.setCategory(category);
        }
        if (approvalType != null) {
            if (current.isApprovalLocked()) {
                throw new InvalidStateTransitionException("Approval type is locked and cannot be changed.");
            }
            current.setApprovalType(approvalType);
        }
        if (scheduledStart != null) {
            current.setScheduledStart(scheduledStart);
        }
        if (scheduledEnd != null) {
            current.setScheduledEnd(scheduledEnd);
        }
        if (affectedSystems != null) {
            current.setAffectedSystems(normalizeAffectedSystems(affectedSystems));
        }

        ChangeRequestEntity updated = changeRequestRepository.save(current);
        logActivity(updated, updated.getCreatedBy(), "CR_UPDATED", Map.of("status", updated.getStatus().name()));
        initializeCreator(updated);
        return updated;
    }

    public ChangeRequestEntity submit(UUID id, UUID actorUserId, String actorRole) {
        ChangeRequestEntity changeRequest = getById(id);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        changeRequest.submit();
        changeRequest.setSlaDeadline(OffsetDateTime.now().plusHours(resolveSlaHours(changeRequest.getPriority())));
        ChangeRequestEntity submitted = changeRequestRepository.save(changeRequest);
        logActivity(submitted, submitted.getCreatedBy(), "CR_SUBMITTED", Map.of("status", submitted.getStatus().name()));
        initializeCreator(submitted);
        return submitted;
    }

    public void cancel(UUID id, UUID actorUserId, String actorRole) {
        ChangeRequestEntity changeRequest = getById(id);
        assertCanMutate(changeRequest, actorUserId, actorRole);
        changeRequest.cancel();
        ChangeRequestEntity cancelled = changeRequestRepository.save(changeRequest);
        logActivity(cancelled, cancelled.getCreatedBy(), "CR_CANCELLED", Map.of("status", cancelled.getStatus().name()));
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequestEntity> list(ChangeRequestStatus status,
                                          Priority priority,
                                          String category,
                                          UUID createdBy,
                                          Pageable pageable) {
        return changeRequestRepository.findAllFiltered(status, priority, category, createdBy, pageable)
                .map(changeRequest -> {
                    initializeCreator(changeRequest);
                    return changeRequest;
                });
    }

    @Transactional(readOnly = true)
    public ChangeRequestEntity getById(UUID id) {
        ChangeRequestEntity changeRequest = changeRequestRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Change request not found."));
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
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "User not found."));

        if (crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, userId).isPresent()) {
            throw new DomainNotPermittedException("DUPLICATE_APPROVER", "User is already an approver on this CR.");
        }

        int position = crApproverRepository.countByChangeRequestId(changeRequestId) + 1;
        CrApproverEntity approver = new CrApproverEntity(changeRequest, user, isRequired, position, true);
        CrApproverEntity created = crApproverRepository.save(approver);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVER_ADDED",
            Map.of("approverId", created.getId().toString(), "userId", userId.toString()));
        return created;
    }

    public void removeApprover(UUID changeRequestId, UUID approverId) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        if (changeRequest.isApprovalLocked()) {
            throw new InvalidStateTransitionException("Approvers are locked and cannot be changed.");
        }

        CrApproverEntity approver = crApproverRepository.findById(approverId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Approver not found."));
        if (!approver.getChangeRequest().getId().equals(changeRequestId)) {
            throw new DomainNotPermittedException("NOT_FOUND", "Approver not found on this change request.");
        }

        crApproverRepository.delete(approver);
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVER_REMOVED",
            Map.of("approverId", approverId.toString()));
        resequenceApprovers(changeRequestId);
    }

    public List<CrApproverEntity> reorderApprovers(UUID changeRequestId, List<UUID> approverIds) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        if (changeRequest.isApprovalLocked()) {
            throw new InvalidStateTransitionException("Approvers are locked and cannot be changed.");
        }

        List<CrApproverEntity> existing = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        if (existing.size() != approverIds.size()) {
            throw new DomainNotPermittedException("INVALID_ORDER", "Approver order payload does not match approver set.");
        }

        for (int i = 0; i < approverIds.size(); i++) {
            UUID approverId = approverIds.get(i);
            CrApproverEntity approver = existing.stream()
                    .filter(a -> a.getId().equals(approverId))
                    .findFirst()
                    .orElseThrow(() -> new DomainNotPermittedException("INVALID_ORDER", "Approver order contains unknown IDs."));
            approver.setPosition(i + 1);
        }

        List<CrApproverEntity> saved = crApproverRepository.saveAll(existing).stream()
                .sorted(Comparator.comparingInt(CrApproverEntity::getPosition))
                .toList();
        logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_APPROVERS_REORDERED",
            Map.of("count", saved.size()));
        return saved;
    }

    public ChangeRequestEntity approve(UUID changeRequestId, UUID actorUserId) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        ensurePendingApproval(changeRequest);

        CrApproverEntity approver = crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, actorUserId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER", "You are not an approver on this change request."));
        UserEntity actor = approver.getUser();

        if (changeRequest.getApprovalType() == ApprovalType.LINEAR) {
            CrApproverEntity next = nextPendingApprover(changeRequestId);
            if (next != null && !next.getId().equals(approver.getId())) {
                throw new DomainNotPermittedException("OUT_OF_SEQUENCE", "Only the next pending approver can act in LINEAR mode.");
            }
        }

        approver.approve();
        changeRequest.markApprovalLocked();
        changeRequest.evaluateApprovalClosure();
        crApproverRepository.save(approver);
        ChangeRequestEntity updated = changeRequestRepository.save(changeRequest);
        logActivity(updated, actor, "CR_APPROVED", Map.of("approverId", approver.getId().toString(), "status", updated.getStatus().name()));
        initializeCreator(updated);
        return updated;
    }

    public ChangeRequestEntity reject(UUID changeRequestId, UUID actorUserId, String reason) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
        ensurePendingApproval(changeRequest);

        CrApproverEntity approver = crApproverRepository.findByChangeRequestIdAndUserId(changeRequestId, actorUserId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_APPROVER", "You are not an approver on this change request."));
        UserEntity actor = approver.getUser();

        if (changeRequest.getApprovalType() == ApprovalType.LINEAR) {
            CrApproverEntity next = nextPendingApprover(changeRequestId);
            if (next != null && !next.getId().equals(approver.getId())) {
                throw new DomainNotPermittedException("OUT_OF_SEQUENCE", "Only the next pending approver can act in LINEAR mode.");
            }
        }

        approver.reject(reason);
        changeRequest.markApprovalLocked();
        changeRequest.evaluateApprovalClosure();
        crApproverRepository.save(approver);
        ChangeRequestEntity updated = changeRequestRepository.save(changeRequest);
        logActivity(updated, actor, "CR_REJECTED", Map.of("approverId", approver.getId().toString(), "reason", reason, "status", updated.getStatus().name()));
        initializeCreator(updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<ActivityStreamEntity> listActivity(UUID changeRequestId) {
        getById(changeRequestId);
        return activityStreamRepository.findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
    }

    @Transactional(readOnly = true)
    public List<AttachmentEntity> listAttachments(UUID changeRequestId) {
        getById(changeRequestId);
        return attachmentRepository.findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
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
        if (!isSignatureValid(file, mimeType)) {
            throw new DomainNotPermittedException("INVALID_FILE_CONTENT", "Attachment content does not match file type.");
        }

        UserEntity uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Uploader not found."));

        String tenant = TenantContext.getCurrentTenant();
        String safeOriginalName = file.getOriginalFilename() == null ? "attachment.bin" : file.getOriginalFilename();
        String storedName = UUID.randomUUID() + "-" + safeOriginalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        Path storageDir = Path.of(storageBasePath, tenant == null ? "public" : tenant, changeRequestId.toString());
        Path outputPath = storageDir.resolve(storedName);

        try {
            Files.createDirectories(storageDir);
            Files.copy(file.getInputStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new DomainNotPermittedException("UPLOAD_FAILED", "Could not persist uploaded file.");
        }

        AttachmentEntity attachment = new AttachmentEntity(
                changeRequest,
                uploader,
                safeOriginalName,
                mimeType,
                file.getSize(),
                outputPath.toString()
        );
        AttachmentEntity saved = attachmentRepository.save(attachment);
        logActivity(changeRequest, uploader, "CR_ATTACHMENT_UPLOADED", Map.of(
                "attachmentId", saved.getId().toString(),
                "fileName", safeOriginalName
        ));
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
            payload.put("count", saved.size());
            logActivity(changeRequest, changeRequest.getCreatedBy(), "CR_CUSTOM_FIELDS_UPDATED", payload);
            return saved;
    }

    public record FieldValue(UUID fieldId, String value) {}

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
        List<CrApproverEntity> approvers = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(changeRequestId);
        for (int i = 0; i < approvers.size(); i++) {
            approvers.get(i).setPosition(i + 1);
        }
        crApproverRepository.saveAll(approvers);
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

    private boolean isSignatureValid(MultipartFile file, String mimeType) {
        if (mimeType == null) {
            return false;
        }
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(8);
            if (mimeType.equalsIgnoreCase("application/pdf")) {
                return startsWith(header, new byte[] {0x25, 0x50, 0x44, 0x46}); // %PDF
            }
            if (mimeType.equalsIgnoreCase("image/png")) {
                return startsWith(header, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
            }
            if (mimeType.equalsIgnoreCase("image/jpeg")) {
                return startsWith(header, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            }
            if (mimeType.equalsIgnoreCase("application/zip")) {
                return startsWith(header, new byte[] {0x50, 0x4B, 0x03, 0x04});
            }
            // For text-like types, allow as long as content type is allowlisted.
            return mimeType.startsWith("text/") || mimeType.equalsIgnoreCase("application/json");
        } catch (IOException ex) {
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

    private String[] normalizeAffectedSystems(String[] affectedSystems) {
        if (affectedSystems == null || affectedSystems.length == 0) {
            return new String[0];
        }
        return java.util.Arrays.stream(affectedSystems)
                .filter(s -> s != null && !s.isBlank())
                .toArray(String[]::new);
    }

    private long resolveSlaHours(Priority priority) {
        if (priority == null) {
            return 48;
        }
        return switch (priority) {
            case CRITICAL -> 8;
            case HIGH -> 24;
            case MEDIUM -> 48;
            case LOW -> 72;
        };
    }
}