package io.audita.api.dto.response;

public record PlatformHealthResponse(
        String status,
        int availabilityPercent,
        String detail
) {
}