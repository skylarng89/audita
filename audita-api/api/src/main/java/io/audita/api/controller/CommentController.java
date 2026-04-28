package io.audita.api.controller;

import io.audita.api.dto.request.CreateCommentRequest;
import io.audita.api.dto.response.CommentResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/change-requests/{changeRequestId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CommentResponse> list(@PathVariable UUID changeRequestId) {
        return commentService.list(changeRequestId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> create(@PathVariable UUID changeRequestId,
                                                  @Valid @RequestBody CreateCommentRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        var created = commentService.create(changeRequestId, principal.userId(), req.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(created));
    }
}
