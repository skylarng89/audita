package io.audita.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GroupMemberRequest(
        @NotNull UUID userId
) {}
