package io.audita.api.controller;

import io.audita.api.dto.request.AddApproverRequest;
import io.audita.api.dto.request.CreateChangeRequestRequest;
import io.audita.api.dto.request.RejectChangeRequestRequest;
import io.audita.api.dto.request.ReorderApproversRequest;
import io.audita.api.dto.request.UpsertChangeRequestCustomFieldsRequest;
import io.audita.api.dto.response.ActivityStreamResponse;
import io.audita.api.dto.request.UpdateChangeRequestRequest;
import io.audita.api.dto.response.ChangeRequestCustomFieldResponse;
import io.audita.api.dto.response.ChangeRequestResponse;
import io.audita.api.dto.response.CrApproverResponse;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.infrastructure.service.ChangeRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/change-requests")
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    public ChangeRequestController(ChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ChangeRequestResponse> create(
            @Valid @RequestBody CreateChangeRequestRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        UUID createdById = UUID.fromString(principal.getUsername());
        var created = changeRequestService.create(
            req.title(),
            req.description(),
            req.priority(),
            req.riskLevel(),
            req.category(),
            req.approvalType(),
            req.scheduledStart(),
            req.scheduledEnd(),
            req.affectedSystems() == null ? null : req.affectedSystems().toArray(String[]::new),
            createdById
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ChangeRequestResponse.from(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateChangeRequestRequest req) {
        return ChangeRequestResponse.from(changeRequestService.update(
            id,
            req.title(),
            req.description(),
            req.priority(),
            req.riskLevel(),
            req.category(),
            req.approvalType(),
            req.scheduledStart(),
            req.scheduledEnd(),
            req.affectedSystems() == null ? null : req.affectedSystems().toArray(String[]::new)
        ));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse submit(@PathVariable UUID id) {
        return ChangeRequestResponse.from(changeRequestService.submit(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        changeRequestService.cancel(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ChangeRequestResponse> list(
            @RequestParam(required = false) ChangeRequestStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID createdBy,
            @PageableDefault(size = 20) Pageable pageable) {

        return changeRequestService.list(status, priority, category, createdBy, pageable)
                .map(ChangeRequestResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ChangeRequestResponse get(@PathVariable UUID id) {
        return ChangeRequestResponse.from(changeRequestService.getById(id));
    }

    @GetMapping("/{id}/approvers")
    @PreAuthorize("isAuthenticated()")
    public List<CrApproverResponse> listApprovers(@PathVariable UUID id) {
        return changeRequestService.listApprovers(id).stream()
                .map(CrApproverResponse::from)
                .toList();
    }

    @PostMapping("/{id}/approvers")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CrApproverResponse> addApprover(@PathVariable UUID id,
                                                           @Valid @RequestBody AddApproverRequest req) {
        var created = changeRequestService.addApprover(id, req.userId(), req.isRequired());
        return ResponseEntity.status(HttpStatus.CREATED).body(CrApproverResponse.from(created));
    }

    @PatchMapping("/{id}/approvers/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public List<CrApproverResponse> reorderApprovers(@PathVariable UUID id,
                                                     @Valid @RequestBody ReorderApproversRequest req) {
        return changeRequestService.reorderApprovers(id, req.approverIds()).stream()
                .map(CrApproverResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}/approvers/{approverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeApprover(@PathVariable UUID id, @PathVariable UUID approverId) {
        changeRequestService.removeApprover(id, approverId);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse approve(@PathVariable UUID id,
                                         @AuthenticationPrincipal UserDetails principal) {
        return ChangeRequestResponse.from(changeRequestService.approve(id, UUID.fromString(principal.getUsername())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN', 'SUPER_ADMIN')")
    public ChangeRequestResponse reject(@PathVariable UUID id,
                                        @Valid @RequestBody RejectChangeRequestRequest req,
                                        @AuthenticationPrincipal UserDetails principal) {
        return ChangeRequestResponse.from(changeRequestService.reject(id, UUID.fromString(principal.getUsername()), req.reason()));
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
            @Valid @RequestBody UpsertChangeRequestCustomFieldsRequest req) {

        List<ChangeRequestService.FieldValue> fields = req.fields().stream()
                .map(f -> new ChangeRequestService.FieldValue(f.fieldId(), f.value()))
                .toList();

        return changeRequestService.upsertCustomFields(id, fields).stream()
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
}