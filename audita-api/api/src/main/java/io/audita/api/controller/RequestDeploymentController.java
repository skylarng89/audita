package io.audita.api.controller;

import io.audita.api.dto.request.CreateCommentRequest;
import io.audita.api.dto.request.RejectChangeRequestRequest;
import io.audita.api.dto.response.RequestDeploymentResponse;
import io.audita.api.dto.response.RequestDeploymentResponse.DeploymentApproverResponse;
import io.audita.api.dto.response.StageCommentResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.persistence.entity.RequestDeploymentApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.service.RequestDeploymentService;
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
@RequestMapping("/api/v1/change-requests/{requestId}/deployment")
public class RequestDeploymentController {

    private final RequestDeploymentService requestDeploymentService;

    public RequestDeploymentController(RequestDeploymentService requestDeploymentService) {
        this.requestDeploymentService = requestDeploymentService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public RequestDeploymentResponse get(@PathVariable UUID requestId) {
        RequestDeploymentEntity deployment = requestDeploymentService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        List<RequestDeploymentApproverEntity> approverEntities =
                requestDeploymentService.listApprovers(deployment.getId());
        Map<UUID, UserEntity> approverUsers =
                requestDeploymentService.loadApproverUsers(approverEntities);
        return RequestDeploymentResponse.from(deployment, approverEntities, approverUsers);
    }

    @GetMapping("/approvers")
    @PreAuthorize("isAuthenticated()")
    public List<DeploymentApproverResponse> listApprovers(@PathVariable UUID requestId) {
        RequestDeploymentEntity deployment = requestDeploymentService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        List<RequestDeploymentApproverEntity> approverEntities =
                requestDeploymentService.listApprovers(deployment.getId());
        Map<UUID, UserEntity> approverUsers =
                requestDeploymentService.loadApproverUsers(approverEntities);
        return approverEntities.stream()
                .map(a -> DeploymentApproverResponse.from(a, approverUsers.get(a.getUserId())))
                .toList();
    }

    @PostMapping("/approve")
    @PreAuthorize("isAuthenticated() and !hasRole('AUDITOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestDeploymentEntity deployment = requestDeploymentService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        requestDeploymentService.approveDeployment(deployment.getId(), principal.userId());
    }

    @PostMapping("/reject")
    @PreAuthorize("isAuthenticated() and !hasRole('AUDITOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectChangeRequestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestDeploymentEntity deployment = requestDeploymentService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        requestDeploymentService.rejectDeployment(deployment.getId(), principal.userId(), req.reason());
    }

    @GetMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public List<StageCommentResponse> listComments(@PathVariable UUID requestId) {
        RequestDeploymentEntity deployment = requestDeploymentService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        List<RequestDeploymentCommentEntity> comments =
                requestDeploymentService.listComments(deployment.getId());
        Map<UUID, UserEntity> authors = requestDeploymentService.loadAuthors(comments);
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
        RequestDeploymentEntity deployment = requestDeploymentService.getByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        RequestDeploymentCommentEntity saved = requestDeploymentService.createComment(
                deployment.getId(), principal.userId(), req.body());
        Map<UUID, UserEntity> authors = requestDeploymentService.loadAuthors(List.of(saved));
        return new ResponseEntity<>(
                StageCommentResponse.from(saved, authors.get(saved.getAuthorId())),
                HttpStatus.CREATED);
    }
}
