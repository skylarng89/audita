package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        List<UUID> memberIds,
        @NotNull Boolean isActive,
        int displayOrder
) {}
