package io.audita.api.dto.request;

import io.audita.domain.model.RequestWorkflowMode;
import jakarta.validation.constraints.NotNull;

public record SetWorkflowModeRequest(
        @NotNull RequestWorkflowMode workflowMode
) {}
