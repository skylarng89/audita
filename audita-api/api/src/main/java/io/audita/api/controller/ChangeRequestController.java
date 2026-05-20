package io.audita.api.controller;

import io.audita.api.dto.request.AddApproverRequest;
import io.audita.api.dto.request.AddApproverGroupRequest;
import io.audita.api.dto.request.CreateChangeRequestRequest;
import io.audita.api.dto.request.RejectChangeRequestRequest;
import io.audita.api.dto.request.ReorderApproversRequest;
import io.audita.api.dto.request.UpsertChangeRequestCustomFieldsRequest;
import io.audita.api.dto.response.ActivityStreamResponse;
import io.audita.api.dto.response.AttachmentResponse;
import io.audita.api.dto.request.UpdateChangeRequestRequest;
import io.audita.api.dto.response.ChangeRequestCustomFieldResponse;
import io.audita.api.dto.response.PageResponse;
import io.audita.api.dto.response.ChangeRequestResponse;
import io.audita.api.dto.response.ApproverCandidateResponse;
import io.audita.api.dto.response.CrApproverResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.infrastructure.service.ChangeRequestService;
import io.audita.infrastructure.service.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/change-requests")
public class ChangeRequestController {

    private static final String OPERATION_CREATE = "cr:create";
    private static final String OPERATION_SUBMIT_PREFIX = "cr:submit:";

    private final ChangeRequestService changeRequestService;
    private final IdempotencyService idempotencyService;

    public ChangeRequestController(ChangeRequestService changeRequestService,
                                   IdempotencyService idempotencyService) {
        this.changeRequestService = changeRequestService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ChangeRequestResponse> create(
            @Valid @RequestBody CreateChangeRequestRequest req,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID createdById = principal.userId();
        var existingResource = idempotencyService.findResourceId(createdById, OPERATION_CREATE, idempotencyKey);
        if (existingResource.isPresent()) {
            ChangeRequestResponse replay = ChangeRequestResponse.from(
                    changeRequestService.getById(existingResource.get(), createdById));
            return ResponseEntity.ok(replay);
        }

        var created = changeRequestService.create(new ChangeRequestService.CreateRequest(
                req.title(),
                req.description(),
                req.priority(),
                req.riskLevel(),
                req.category(),
                req.approvalType() != null ? req.approvalType() : ApprovalType.NON_LINEAR,
                req.scheduledStart(),
                req.scheduledEnd(),
                req.affectedSystems(),
                createdById));
        idempotencyService.recordResource(createdById, OPERATION_CREATE, idempotencyKey, created.getId());
        return new ResponseEntity<>(ChangeRequestResponse.from(created), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse update(@PathVariable UUID id,
            @Valid @RequestBody UpdateChangeRequestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ChangeRequestResponse.from(changeRequestService.update(new ChangeRequestService.UpdateRequest(
                id,
                req.title(),
                req.description(),
                req.priority(),
                req.riskLevel(),
                req.category(),
                req.approvalType(),
                req.scheduledStart(),
                req.scheduledEnd(),
                req.affectedSystems(),
                principal.userId(),
                principal.role())));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse submit(@PathVariable UUID id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {
        String operation = OPERATION_SUBMIT_PREFIX + id;
        var existingResource = idempotencyService.findResourceId(principal.userId(), operation, idempotencyKey);
        if (existingResource.isPresent()) {
            return ChangeRequestResponse.from(changeRequestService.getById(existingResource.get(), principal.userId()));
        }

        ChangeRequestResponse submitted = ChangeRequestResponse.from(
                changeRequestService.submit(id, principal.userId(), principal.role()));
        idempotencyService.recordResource(principal.userId(), operation, idempotencyKey, id);
        return submitted;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        changeRequestService.cancel(id, principal.userId(), principal.role());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<ChangeRequestResponse> list(
            @RequestParam(required = false) ChangeRequestStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID createdBy,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        return PageResponse.from(
                changeRequestService.list(status, priority, category, createdBy, principal.userId(), pageable),
                ChangeRequestResponse::from);
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public List<String> listCategories() {
        return changeRequestService.listCategories();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ChangeRequestResponse get(@PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ChangeRequestResponse.from(changeRequestService.getById(id, principal.userId()));
    }

    @GetMapping("/{id}/approvers")
    @PreAuthorize("isAuthenticated()")
    public List<CrApproverResponse> listApprovers(@PathVariable UUID id) {
        return changeRequestService.listApprovers(id).stream()
                .map(CrApproverResponse::from)
                .toList();
    }

    @GetMapping("/approver-candidates")
    @PreAuthorize("hasAnyRole('REQUESTER', 'APPROVER', 'AUDITOR', 'ADMIN', 'SUPER_ADMIN')")
    public List<ApproverCandidateResponse> searchApproverCandidates(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int limit) {
        return changeRequestService.searchApproverCandidates(query, limit).stream()
                .map(ApproverCandidateResponse::from)
                .toList();
    }

    @PostMapping("/{id}/approvers")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CrApproverResponse> addApprover(@PathVariable UUID id,
            @Valid @RequestBody AddApproverRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        var created = changeRequestService.addApprover(
                id,
                req.userId(),
                req.isRequired(),
                principal.userId(),
                principal.role());
        return new ResponseEntity<>(CrApproverResponse.from(created), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/approvers/groups")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<CrApproverResponse>> addApproverGroup(@PathVariable UUID id,
            @Valid @RequestBody AddApproverGroupRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CrApproverResponse> created = changeRequestService.addApproverGroup(
                id,
                req.groupId(),
                req.isRequired(),
                principal.userId(),
                principal.role()).stream().map(CrApproverResponse::from).toList();
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/approvers/reorder")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public List<CrApproverResponse> reorderApprovers(@PathVariable UUID id,
            @Valid @RequestBody ReorderApproversRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return changeRequestService.reorderApprovers(id, req.approverIds(), principal.userId(), principal.role())
                .stream()
                .map(CrApproverResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}/approvers/{approverId}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeApprover(@PathVariable UUID id,
            @PathVariable UUID approverId,
            @AuthenticationPrincipal UserPrincipal principal) {
        changeRequestService.removeApprover(id, approverId, principal.userId(), principal.role());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated() and !hasRole('AUDITOR')")
    public ChangeRequestResponse approve(@PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ChangeRequestResponse.from(changeRequestService.approve(id, principal.userId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated() and !hasRole('AUDITOR')")
    public ChangeRequestResponse reject(@PathVariable UUID id,
            @Valid @RequestBody RejectChangeRequestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ChangeRequestResponse.from(changeRequestService.reject(id, principal.userId(), req.reason()));
    }

    @GetMapping("/{id}/custom-fields")
    @PreAuthorize("isAuthenticated()")
    public List<ChangeRequestCustomFieldResponse> listCustomFields(@PathVariable UUID id) {
        return changeRequestService.listCustomFields(id).stream()
                .map(ChangeRequestCustomFieldResponse::from)
                .toList();
    }

    @PutMapping("/{id}/custom-fields")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public List<ChangeRequestCustomFieldResponse> upsertCustomFields(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertChangeRequestCustomFieldsRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ChangeRequestService.FieldValue> fields = req.fields().stream()
                .map(f -> new ChangeRequestService.FieldValue(f.fieldId(), f.value()))
                .toList();

        return changeRequestService.upsertCustomFields(id, fields, principal.userId(), principal.role()).stream()
                .map(ChangeRequestCustomFieldResponse::from)
                .toList();
    }

    @GetMapping("/{id}/activity")
    @PreAuthorize("isAuthenticated()")
    public List<ActivityStreamResponse> activity(@PathVariable UUID id) {
        return changeRequestService.listActivity(id).stream()
                .map(ActivityStreamResponse::from)
                .toList();
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("isAuthenticated()")
    public List<AttachmentResponse> attachments(@PathVariable UUID id) {
        return changeRequestService.listAttachments(id).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @PostMapping(value = "/{id}/attachments", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        var saved = changeRequestService.uploadAttachment(id, principal.userId(), principal.role(), file);
        return new ResponseEntity<>(AttachmentResponse.from(saved), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {

        var dl = changeRequestService.downloadAttachment(id, attachmentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(dl.mimeType()));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(dl.fileName()).build());
        headers.setContentLength(dl.sizeBytes());

        return new ResponseEntity<>(new InputStreamResource(dl.stream()), headers, HttpStatus.OK);
    }
}
