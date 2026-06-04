package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequestUatRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 10000) String details
) {}
