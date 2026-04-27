package io.audita.api.dto.request;

import io.audita.domain.model.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertSsoConfigRequest(
        @NotNull OAuthProvider provider,
        @NotBlank @Size(max = 500) String clientId,
        @NotBlank String clientSecret,
        @Size(max = 255) String msTenantId
) {}
