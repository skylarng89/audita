package io.audita.api.dto.response;

import io.audita.infrastructure.service.ChangeRequestService;

import java.util.UUID;

public record ApproverCandidateResponse(
        UUID id,
        String kind,
        String label,
        String secondary) {
    public static ApproverCandidateResponse from(ChangeRequestService.ApproverCandidate candidate) {
        return new ApproverCandidateResponse(
                candidate.id(),
                candidate.kind(),
                candidate.label(),
                candidate.secondary());
    }
}