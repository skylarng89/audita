package io.audita.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProvisionTenantRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9-]+", message = "Slug may only contain lowercase letters, digits and hyphens") String slug,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(max = 255) String adminFullName
) {}
