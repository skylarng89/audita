package io.audita.api.dto.request;

import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record UpdateChangeRequestRequest(
        @Size(max = 500) String title,
        String description,
        Priority priority,
        RiskLevel riskLevel,
        @Size(max = 255) String category,
        ApprovalType approvalType,
        OffsetDateTime scheduledStart,
        OffsetDateTime scheduledEnd,
        List<String> affectedSystems
) {}