package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank(message = "Comment body is required")
        String body
) {
}
