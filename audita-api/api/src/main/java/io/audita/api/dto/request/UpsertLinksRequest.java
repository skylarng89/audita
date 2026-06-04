package io.audita.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpsertLinksRequest(
        @NotNull List<UUID> linkedRequestIds
) {}
