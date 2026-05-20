package io.audita.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddApproverGroupRequest(
        @NotNull UUID groupId,
        @NotNull Boolean isRequired) {
}