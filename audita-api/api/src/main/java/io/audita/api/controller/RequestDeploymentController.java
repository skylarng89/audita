package io.audita.api.controller;

import io.audita.api.dto.request.AssignDeployerRequest;
import io.audita.api.dto.request.CreateCommentRequest;
import io.audita.api.dto.response.RequestDeploymentResponse;
import io.audita.api.dto.response.StageCommentResponse;
import io.audita.api.security.UserPrincipal;
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
    public RequestDeploymentResponse get(@PathVariable UUID requestId) {
        RequestDeploymentEntity deployment = requestDeploymentService.getDeploymentWithAssignee(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        UserEntity assignee = deployment.getAssignee();
        String createdByFullName = requestDeploymentService.resolveUserFullName(deployment.getCreatedBy());
        return RequestDeploymentResponse.from(deployment, assignee, createdByFullName);
    }

    @PostMapping("/assignee")
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
    public RequestDeploymentResponse assignDeployer(
            @PathVariable UUID requestId,
            @Valid @RequestBody AssignDeployerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestDeploymentEntity updated = requestDeploymentService.assignDeployer(
                requestId, req.userId(), principal.userId(), principal.role());
        UserEntity assignee = updated.getAssignee();
        String createdByFullName = requestDeploymentService.resolveUserFullName(updated.getCreatedBy());
        return RequestDeploymentResponse.from(updated, assignee, createdByFullName);
    }

    @PostMapping("/complete")
    @PreAuthorize("@authz.hasPermission(authentication, 'deployment.execute')")
    public RequestDeploymentResponse complete(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        RequestDeploymentEntity updated = requestDeploymentService.completeDeployment(
                requestId, principal.userId(), principal.role());
        UserEntity assignee = updated.getAssignee();
        String createdByFullName = requestDeploymentService.resolveUserFullName(updated.getCreatedBy());
        return RequestDeploymentResponse.from(updated, assignee, createdByFullName);
    }

    @GetMapping("/comments")
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
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
    @PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
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
