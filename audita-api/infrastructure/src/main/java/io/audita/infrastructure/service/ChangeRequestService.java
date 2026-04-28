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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final CrApproverRepository crApproverRepository;
    private final ChangeRequestCustomFieldRepository customFieldRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;

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
                                      String[] affectedSystems) {
        ChangeRequestEntity current = getById(id);
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

    public ChangeRequestEntity submit(UUID id) {
        ChangeRequestEntity changeRequest = getById(id);
        changeRequest.submit();
        changeRequest.setSlaDeadline(OffsetDateTime.now().plusHours(resolveSlaHours(changeRequest.getPriority())));
        ChangeRequestEntity submitted = changeRequestRepository.save(changeRequest);
        logActivity(submitted, submitted.getCreatedBy(), "CR_SUBMITTED", Map.of("status", submitted.getStatus().name()));
        initializeCreator(submitted);
        return submitted;
    }

    public void cancel(UUID id) {
        ChangeRequestEntity changeRequest = getById(id);
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
        return changeRequestRepository.findAllFiltered(status, priority, category, createdBy, pageable);
    }

    @Transactional(readOnly = true)
    public ChangeRequestEntity getById(UUID id) {
        return changeRequestRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Change request not found."));
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

    public AttachmentEntity uploadAttachment(UUID changeRequestId, UUID uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainNotPermittedException("EMPTY_FILE", "Attachment file is required.");
        }

        ChangeRequestEntity changeRequest = getById(changeRequestId);
        UserEntity uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Uploader not found."));

        String tenant = TenantContext.getCurrentTenant();
        String safeOriginalName = file.getOriginalFilename() == null ? "attachment.bin" : file.getOriginalFilename();
        String storedName = UUID.randomUUID() + "-" + safeOriginalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        Path storageDir = Path.of("/tmp/audita/uploads", tenant == null ? "public" : tenant, changeRequestId.toString());
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
                file.getContentType(),
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
                                                                    List<FieldValue> fields) {
        ChangeRequestEntity changeRequest = getById(changeRequestId);
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