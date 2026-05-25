package io.audita.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SetupRequest(
        @NotBlank @Size(max = 200) String orgName,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{1,100}$") String slug,
        String subdomain,
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12) String password
) {}
