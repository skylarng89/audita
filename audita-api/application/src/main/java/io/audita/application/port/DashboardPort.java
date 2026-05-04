package io.audita.application.port;

public interface DashboardPort {

    DashboardSummary getSummary();

    record DashboardSummary(
            long pendingApprovals,
            long activeChanges,
            long slaRisks,
            double successRate
    ) {
    }
}