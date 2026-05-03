package io.audita.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExchangeSsoCodeRequest(
        @NotBlank String code
) {}
