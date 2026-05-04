package io.audita.api.dto.response;

public record DashboardSummaryResponse(
        long pendingApprovals,
        long activeChanges,
        long slaRisks,
        double successRate
) {
}