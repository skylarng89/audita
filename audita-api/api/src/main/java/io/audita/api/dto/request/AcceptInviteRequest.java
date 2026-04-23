package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank String token,
        @NotBlank String fullName,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {}
