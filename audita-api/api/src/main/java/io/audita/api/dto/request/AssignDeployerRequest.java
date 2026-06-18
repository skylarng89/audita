package io.audita.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDeployerRequest(
        @NotNull UUID userId
) {}
