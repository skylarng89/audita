package io.audita.api.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AddWatchersRequest(
        @NotEmpty List<UUID> userIds
) {}
