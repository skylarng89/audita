package io.audita.api.controller;

import io.audita.api.dto.request.AddApproverRequest;
import io.audita.api.dto.request.AddWatchersRequest;
import io.audita.api.dto.request.CreateCommentRequest;
import io.audita.api.dto.request.CreateRequestUatRequest;
import io.audita.api.dto.request.RejectChangeRequestRequest;
import io.audita.api.dto.response.RequestUatApproverResponse;
import io.audita.api.dto.response.RequestUatResponse;
import io.audita.api.dto.response.RequestUatWatcherResponse;
import io.audita.api.dto.response.StageCommentResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import io.audita.infrastructure.persistence.entity.RequestUatWatcherEntity;
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.edit')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.edit')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
    public ResponseEntity<RequestUatApproverResponse> addApprover(
            @PathVariable UUID requestId,
            @Valid @RequestBody AddApproverRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        var created = requestUatService.addApprover(
                uat.getId(), req.userId(),
                principal.userId(), principal.role());
        return new ResponseEntity<>(RequestUatApproverResponse.from(created), HttpStatus.CREATED);
    }

    @PostMapping("/approvers/{approverId}/demote")
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void demoteUatApprover(
            @PathVariable UUID requestId,
            @PathVariable UUID approverId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requestUatService.moveUatApproverToWatcher(
                requestId, approverId, principal.userId(), principal.effectivePermissions());
    }

    @GetMapping("/watchers")
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
    public List<RequestUatWatcherResponse> listUatWatchers(@PathVariable UUID requestId) {
        List<RequestUatWatcherEntity> watchers = requestUatService.listUatWatchers(requestId);
        Map<UUID, UserEntity> watcherUsers = requestUatService.loadWatcherUsers(watchers);
        return watchers.stream()
                .map(w -> RequestUatWatcherResponse.from(w, watcherUsers.get(w.getUser().getId())))
                .toList();
    }

    @PostMapping("/watchers")
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
    public ResponseEntity<List<RequestUatWatcherResponse>> addUatWatchers(
            @PathVariable UUID requestId,
            @Valid @RequestBody AddWatchersRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RequestUatWatcherEntity> added = requestUatService.addUatWatchers(
                requestId, request.userIds(), principal.userId(), principal.effectivePermissions());
        Map<UUID, UserEntity> watcherUsers = requestUatService.loadWatcherUsers(added);
        return new ResponseEntity<>(
                added.stream()
                        .map(w -> RequestUatWatcherResponse.from(w, watcherUsers.get(w.getUser().getId())))
                        .toList(),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/watchers/{userId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUatWatcher(
            @PathVariable UUID requestId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requestUatService.removeUatWatcher(
                requestId, userId, principal.userId(), principal.effectivePermissions());
    }

    @PostMapping("/watchers/{userId}/promote")
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promoteUatWatcher(
            @PathVariable UUID requestId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requestUatService.moveUatWatcherToApprover(
                requestId, userId, principal.userId(), principal.effectivePermissions());
    }

    @PostMapping("/approve")
    @PreAuthorize("@authz.hasPermission(authentication, 'uat.signoff')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestUatEntity uat = requestUatService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UAT not found"));
        requestUatService.approveUat(uat.getId(), principal.userId());
    }

    @PostMapping("/reject")
    @PreAuthorize("@authz.hasPermission(authentication, 'uat.signoff')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.edit')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
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
