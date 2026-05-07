package io.audita.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CustomFieldDefinitionRequest(
        @NotBlank @Size(max = 255) String label,
        @NotBlank String fieldType,
        List<String> options,
        @NotNull boolean isRequired,
        @Min(0) int displayOrder
) {
}
