package io.audita.api.controller;

import io.audita.api.dto.response.DashboardSummaryResponse;
import io.audita.application.port.DashboardPort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardPort dashboardPort;

    public DashboardController(DashboardPort dashboardPort) {
        this.dashboardPort = dashboardPort;
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public DashboardSummaryResponse getSummary() {
        DashboardPort.DashboardSummary summary = dashboardPort.getSummary();
        return new DashboardSummaryResponse(
            summary.pendingApprovals(),
            summary.activeChanges(),
            summary.slaRisks(),
            summary.successRate()
        );
    }
}