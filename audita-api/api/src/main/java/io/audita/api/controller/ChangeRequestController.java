package io.audita.api.controller;

import io.audita.api.dto.request.AddApproverRequest;
import io.audita.api.dto.request.AddApproverGroupRequest;
import io.audita.api.dto.request.CreateChangeRequestRequest;
import io.audita.api.dto.request.RejectChangeRequestRequest;
import io.audita.api.dto.request.ReorderApproversRequest;
import io.audita.api.dto.request.SetWorkflowModeRequest;
import io.audita.api.dto.request.UpsertChangeRequestCustomFieldsRequest;
import io.audita.api.dto.request.UpsertLinksRequest;
import io.audita.api.dto.response.ActivityStreamResponse;
import io.audita.api.dto.response.AttachmentResponse;
import io.audita.api.dto.request.UpdateChangeRequestRequest;
import io.audita.api.dto.response.ChangeRequestCustomFieldResponse;
import io.audita.api.dto.response.PageResponse;
import io.audita.api.dto.response.ChangeRequestResponse;
import io.audita.api.dto.response.ApproverCandidateResponse;
import io.audita.api.dto.response.CrApproverResponse;
import io.audita.api.dto.response.RequestLinkSearchResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.infrastructure.service.ChangeRequestService;
import io.audita.infrastructure.service.IdempotencyService;
import io.audita.infrastructure.service.RequestLinkService;
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
    private final RequestLinkService requestLinkService;

    public ChangeRequestController(ChangeRequestService changeRequestService,
                                   IdempotencyService idempotencyService,
                                   RequestLinkService requestLinkService) {
        this.changeRequestService = changeRequestService;
        this.idempotencyService = idempotencyService;
        this.requestLinkService = requestLinkService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ChangeRequestResponse> create(
            @Valid @RequestBody CreateChangeRequestRequest req,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID createdById = principal.userId();
        var claimed = idempotencyService.claimIdempotencyKey(createdById, OPERATION_CREATE, idempotencyKey);
        if (claimed.isPresent()) {
            ChangeRequestResponse replay = ChangeRequestResponse.from(
                    changeRequestService.getById(claimed.get(), createdById, principal.role()));
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
                createdById,
                req.workflowMode(),
                req.requestDepartmentId(),
                req.destinationDepartmentId()));
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
                principal.role(),
                req.workflowMode(),
                req.requestDepartmentId(),
                req.destinationDepartmentId())));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse submit(@PathVariable UUID id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {
        String operation = OPERATION_SUBMIT_PREFIX + id;
        var claimed = idempotencyService.claimIdempotencyKey(principal.userId(), operation, idempotencyKey);
        if (claimed.isPresent()) {
            return ChangeRequestResponse.from(
                    changeRequestService.getById(claimed.get(), principal.userId(), principal.role()));
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

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse complete(@PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ChangeRequestResponse.from(
                changeRequestService.completeRequest(id, principal.userId(), principal.role()));
    }

    @PatchMapping("/{id}/workflow-mode")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse setWorkflowMode(@PathVariable UUID id,
            @Valid @RequestBody SetWorkflowModeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ChangeRequestResponse.from(
                changeRequestService.setWorkflowMode(id, req.workflowMode(), principal.userId(), principal.role()));
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
                changeRequestService.list(status, priority, category, createdBy, principal.userId(), principal.role(), pageable),
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
        return ChangeRequestResponse.from(changeRequestService.getById(id, principal.userId(), principal.role()));
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

    @PatchMapping("/{id}/approvers/{approverId}/requirement")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public CrApproverResponse updateApproverRequirement(@PathVariable UUID id,
            @PathVariable UUID approverId,
            @RequestParam boolean isRequired,
            @AuthenticationPrincipal UserPrincipal principal) {
        var updated = changeRequestService.updateApproverRequirement(id, approverId, isRequired,
                principal.userId(), principal.role());
        return CrApproverResponse.from(updated);
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

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public List<RequestLinkSearchResponse> searchRequests(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int limit) {
        return requestLinkService.searchRequests(query, limit).stream()
                .map(RequestLinkSearchResponse::from)
                .toList();
    }

    @GetMapping("/{id}/links")
    @PreAuthorize("isAuthenticated()")
    public List<UUID> getLinkedRequests(@PathVariable UUID id) {
        return requestLinkService.getLinkedRequests(id);
    }

    @PutMapping("/{id}/links")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsertLinks(@PathVariable UUID id,
            @Valid @RequestBody UpsertLinksRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        requestLinkService.upsertLinks(id, req.linkedRequestIds(), principal.userId());
    }
}
