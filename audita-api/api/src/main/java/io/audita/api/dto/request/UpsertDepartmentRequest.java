package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertDepartmentRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 32) String code,
        boolean isActive,
        int displayOrder
) {}
