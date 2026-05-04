package io.audita.infrastructure.service;

import io.audita.application.port.DashboardPort;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService implements DashboardPort {

    private final ChangeRequestRepository changeRequestRepository;

    public DashboardService(ChangeRequestRepository changeRequestRepository) {
        this.changeRequestRepository = changeRequestRepository;
    }

    @Override
    public DashboardSummary getSummary() {
        long pendingApprovals = changeRequestRepository.countByStatus(ChangeRequestStatus.PENDING_APPROVAL);
        long draft = changeRequestRepository.countByStatus(ChangeRequestStatus.DRAFT);
        long activeChanges = pendingApprovals + draft;
        long slaRisks = changeRequestRepository.countByStatusInAndSlaBreachedTrue(
                List.of(ChangeRequestStatus.DRAFT, ChangeRequestStatus.PENDING_APPROVAL)
        );

        long approved = changeRequestRepository.countByStatus(ChangeRequestStatus.APPROVED);
        long closed = approved
                + changeRequestRepository.countByStatus(ChangeRequestStatus.REJECTED)
                + changeRequestRepository.countByStatus(ChangeRequestStatus.CANCELLED);
        double successRate = closed == 0 ? 0.0 : (approved * 100.0) / closed;

        return new DashboardSummary(pendingApprovals, activeChanges, slaRisks, successRate);
    }
}