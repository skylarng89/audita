package io.audita.api.controller;

import io.audita.api.dto.request.AddApproverRequest;
import io.audita.api.dto.request.CreateCommentRequest;
import io.audita.api.dto.request.CreateRequestUatRequest;
import io.audita.api.dto.request.RejectChangeRequestRequest;
import io.audita.api.dto.response.RequestUatApproverResponse;
import io.audita.api.dto.response.RequestUatResponse;
import io.audita.api.dto.response.StageCommentResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.service.RequestUatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/change-requests/{requestId}/uat")
public class RequestUatController {

    private final RequestUatService requestUatService;

    public RequestUatController(RequestUatService requestUatService) {
        this.requestUatService = requestUatService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public RequestUatResponse get(@PathVariable UUID requestId) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        List<RequestUatApproverEntity> approverEntities = requestUatService.listApprovers(uat.getId());
        Map<UUID, UserEntity> approverUsers = requestUatService.loadApproverUsers(approverEntities);
        List<RequestUatApproverResponse> approverResponses = approverEntities.stream()
                .map(a -> RequestUatApproverResponse.from(a, approverUsers.get(a.getUserId())))
                .toList();
        String createdByFullName = requestUatService.resolveUserFullName(uat.getCreatedBy());
        return RequestUatResponse.from(uat, createdByFullName, approverResponses);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RequestUatResponse> create(
            @PathVariable UUID requestId,
            @Valid @RequestBody CreateRequestUatRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity created = requestUatService.createUat(
                requestId, req.title(), req.details(),
                principal.userId(), principal.role());
        String createdByFullName = requestUatService.resolveUserFullName(created.getCreatedBy());
        return new ResponseEntity<>(
                RequestUatResponse.from(created, createdByFullName, List.of()),
                HttpStatus.CREATED);
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public RequestUatResponse update(
            @PathVariable UUID requestId,
            @Valid @RequestBody CreateRequestUatRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        RequestUatEntity updated = requestUatService.updateUat(
                uat.getId(), req.title(), req.details(),
                principal.userId(), principal.role());
        List<RequestUatApproverEntity> approverEntities = requestUatService.listApprovers(updated.getId());
        Map<UUID, UserEntity> approverUsers = requestUatService.loadApproverUsers(approverEntities);
        List<RequestUatApproverResponse> approverResponses = approverEntities.stream()
                .map(a -> RequestUatApproverResponse.from(a, approverUsers.get(a.getUserId())))
                .toList();
        String createdByFullName = requestUatService.resolveUserFullName(updated.getCreatedBy());
        return RequestUatResponse.from(updated, createdByFullName, approverResponses);
    }

    @GetMapping("/approvers")
    @PreAuthorize("isAuthenticated()")
    public List<RequestUatApproverResponse> listApprovers(@PathVariable UUID requestId) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        List<RequestUatApproverEntity> approverEntities = requestUatService.listApprovers(uat.getId());
        Map<UUID, UserEntity> approverUsers = requestUatService.loadApproverUsers(approverEntities);
        return approverEntities.stream()
                .map(a -> RequestUatApproverResponse.from(a, approverUsers.get(a.getUserId())))
                .toList();
    }

    @PostMapping("/approvers")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RequestUatApproverResponse> addApprover(
            @PathVariable UUID requestId,
            @Valid @RequestBody AddApproverRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        var created = requestUatService.addApprover(
                uat.getId(), req.userId(), req.isRequired(),
                principal.userId(), principal.role());
        return new ResponseEntity<>(RequestUatApproverResponse.from(created), HttpStatus.CREATED);
    }

    @PatchMapping("/approvers/{approverId}/requirement")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public RequestUatApproverResponse updateApproverRequirement(
            @PathVariable UUID requestId,
            @PathVariable UUID approverId,
            @RequestParam boolean isRequired,
            @AuthenticationPrincipal UserPrincipal principal) {
        var updated = requestUatService.updateApproverRequirement(
                requestId, approverId, isRequired,
                principal.userId(), principal.role());
        Map<UUID, UserEntity> approverUsers = requestUatService.loadApproverUsers(List.of(updated));
        return RequestUatApproverResponse.from(updated, approverUsers.get(updated.getUserId()));
    }

    @PostMapping("/approve")
    @PreAuthorize("isAuthenticated() and !hasRole('AUDITOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        requestUatService.approveUat(uat.getId(), principal.userId());
    }

    @PostMapping("/reject")
    @PreAuthorize("isAuthenticated() and !hasRole('AUDITOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectChangeRequestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        requestUatService.rejectUat(uat.getId(), principal.userId(), req.reason());
    }

    @PostMapping("/promote")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN', 'SUPER_ADMIN')")
    public RequestUatResponse promote(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        RequestUatEntity promoted = requestUatService.promoteToDeployment(
                uat.getId(), principal.userId(), principal.role());
        List<RequestUatApproverEntity> approverEntities = requestUatService.listApprovers(promoted.getId());
        Map<UUID, UserEntity> approverUsers = requestUatService.loadApproverUsers(approverEntities);
        List<RequestUatApproverResponse> approverResponses = approverEntities.stream()
                .map(a -> RequestUatApproverResponse.from(a, approverUsers.get(a.getUserId())))
                .toList();
        String createdByFullName = requestUatService.resolveUserFullName(promoted.getCreatedBy());
        return RequestUatResponse.from(promoted, createdByFullName, approverResponses);
    }

    @GetMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public List<StageCommentResponse> listComments(@PathVariable UUID requestId) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        List<RequestUatCommentEntity> comments = requestUatService.listComments(uat.getId());
        Map<UUID, UserEntity> authors = requestUatService.loadAuthors(comments);
        return comments.stream()
                .map(c -> StageCommentResponse.from(c, authors.get(c.getAuthorId())))
                .toList();
    }

    @PostMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StageCommentResponse> createComment(
            @PathVariable UUID requestId,
            @Valid @RequestBody CreateCommentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        RequestUatCommentEntity saved = requestUatService.createComment(
                uat.getId(), principal.userId(), req.body());
        Map<UUID, UserEntity> authors = requestUatService.loadAuthors(List.of(saved));
        return new ResponseEntity<>(
                StageCommentResponse.from(saved, authors.get(saved.getAuthorId())),
                HttpStatus.CREATED);
    }
}
