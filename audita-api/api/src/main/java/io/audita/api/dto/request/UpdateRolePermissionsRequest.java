package io.audita.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateRolePermissionsRequest(
        @NotEmpty List<@NotBlank String> permissionCodes) {
}