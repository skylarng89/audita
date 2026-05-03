package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddDomainRequest(
        @NotBlank @Size(max = 255) String domain
) {}
