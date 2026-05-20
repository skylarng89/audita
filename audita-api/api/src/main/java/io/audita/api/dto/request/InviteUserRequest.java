package io.audita.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record InviteUserRequest(
                @NotBlank @Email String email,
                @NotBlank @Size(max = 255) String fullName,
                UUID roleId,
                List<UUID> roleIds) {
}
