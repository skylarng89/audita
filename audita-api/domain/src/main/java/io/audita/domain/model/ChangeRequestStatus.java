package io.audita.domain.model;

public enum ChangeRequestStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isClosed() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }

    public boolean isEditable() {
        return this == DRAFT || this == PENDING_APPROVAL;
    }
}
