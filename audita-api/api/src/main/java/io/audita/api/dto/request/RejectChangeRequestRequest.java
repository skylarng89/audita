package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectChangeRequestRequest(
        @NotBlank @Size(max = 2000) String reason
) {}
