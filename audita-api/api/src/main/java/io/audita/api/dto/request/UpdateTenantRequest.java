package io.audita.api.dto.request;

import io.audita.domain.model.TenantStatus;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @Size(max = 255) String name,
        TenantStatus status
) {}
