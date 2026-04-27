package io.audita.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpsertChangeRequestCustomFieldsRequest(
        @NotEmpty List<@Valid Item> fields
) {
    public record Item(
            @NotNull UUID fieldId,
            String value
    ) {}
}
