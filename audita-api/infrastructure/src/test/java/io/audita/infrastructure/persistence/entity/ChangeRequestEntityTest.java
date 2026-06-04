package io.audita.infrastructure.persistence.entity;

import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.CompletionStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.domain.model.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChangeRequestEntityTest {

    @Test
    void submitTransitionsDraftToPendingApproval() {
        ChangeRequestEntity changeRequest = baseChangeRequest();

        changeRequest.submit();

        assertEquals(ChangeRequestStatus.PENDING_APPROVAL, changeRequest.getStatus());
    }

    @Test
    void evaluateApprovalClosureApprovesWhenAllRequiredApproved() {
        ChangeRequestEntity changeRequest = baseChangeRequest();
        changeRequest.setStatus(ChangeRequestStatus.PENDING_APPROVAL);

        CrApproverEntity a1 = new CrApproverEntity();
        a1.setRequired(true);
        a1.setStatus(ApproverStatus.APPROVED);
        CrApproverEntity a2 = new CrApproverEntity();
        a2.setRequired(true);
        a2.setStatus(ApproverStatus.APPROVED);

        changeRequest.getApprovers().add(a1);
        changeRequest.getApprovers().add(a2);
        changeRequest.evaluateApprovalClosure();

        assertEquals(ChangeRequestStatus.APPROVED, changeRequest.getStatus());
    }

    @Test
    void evaluateApprovalClosureRejectsWhenSingleRequiredRejected() {
        ChangeRequestEntity changeRequest = baseChangeRequest();
        changeRequest.setStatus(ChangeRequestStatus.PENDING_APPROVAL);

        CrApproverEntity a1 = new CrApproverEntity();
        a1.setRequired(true);
        a1.setStatus(ApproverStatus.REJECTED);
        changeRequest.getApprovers().add(a1);

        changeRequest.evaluateApprovalClosure();

        assertEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus());
    }

    @Test
    void evaluateApprovalClosureApprovesWhenNoRequiredApproversAndAllOptionalApproved() {
        ChangeRequestEntity changeRequest = baseChangeRequest();
        changeRequest.setStatus(ChangeRequestStatus.PENDING_APPROVAL);

        CrApproverEntity a1 = new CrApproverEntity();
        a1.setRequired(false);
        a1.setStatus(ApproverStatus.APPROVED);
        changeRequest.getApprovers().add(a1);

        changeRequest.evaluateApprovalClosure();

        assertEquals(ChangeRequestStatus.APPROVED, changeRequest.getStatus());
    }

    // ── RW15-001: display ID and dual-status fields ────────────────────────────

    @Test
    void displayIdDefaultsToNull() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        // Migration assumption: display_id is nullable; populated by service layer after insert.
        assertNull(changeRequest.getDisplayId());
    }

    @Test
    void displayIdCanBeSetAndRetrieved() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setDisplayId("CR-2026-00042");
        assertEquals("CR-2026-00042", changeRequest.getDisplayId());
    }

    @Test
    void approvalStatusDefaultsToDraft() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        // Migration assumption: approval_status DEFAULT 'DRAFT' mirrors legacy status column.
        assertEquals(ChangeRequestStatus.DRAFT, changeRequest.getApprovalStatus());
    }

    @Test
    void approvalStatusCanBeSetAndRetrieved() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setApprovalStatus(ChangeRequestStatus.PENDING_APPROVAL);
        assertEquals(ChangeRequestStatus.PENDING_APPROVAL, changeRequest.getApprovalStatus());
    }

    @Test
    void completionStatusDefaultsToInProgress() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        // Migration assumption: completion_status DEFAULT 'IN_PROGRESS' for all existing rows.
        assertEquals(CompletionStatus.IN_PROGRESS, changeRequest.getCompletionStatus());
    }

    @Test
    void completionStatusCanBeSetAndRetrieved() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setCompletionStatus(CompletionStatus.COMPLETED);
        assertEquals(CompletionStatus.COMPLETED, changeRequest.getCompletionStatus());
    }

    @Test
    void workflowModeDefaultsToApprovalOnly() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        // Migration assumption: workflow_mode DEFAULT 'APPROVAL_ONLY' preserves legacy behaviour.
        assertEquals(RequestWorkflowMode.APPROVAL_ONLY, changeRequest.getWorkflowMode());
    }

    @Test
    void workflowModeCanBeSetAndRetrieved() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setWorkflowMode(RequestWorkflowMode.DELIVERY_PIPELINE);
        assertEquals(RequestWorkflowMode.DELIVERY_PIPELINE, changeRequest.getWorkflowMode());
    }

    // ── RW15-001: dual-status sync ─────────────────────────────────────────────

    @Test
    void submitSyncsApprovalStatus() {
        ChangeRequestEntity changeRequest = baseChangeRequest();

        changeRequest.submit();

        assertEquals(ChangeRequestStatus.PENDING_APPROVAL, changeRequest.getStatus());
        assertEquals(ChangeRequestStatus.PENDING_APPROVAL, changeRequest.getApprovalStatus());
    }

    @Test
    void evaluateApprovalClosureSyncsApprovalStatus() {
        ChangeRequestEntity changeRequest = baseChangeRequest();
        changeRequest.setStatus(ChangeRequestStatus.PENDING_APPROVAL);
        changeRequest.setApprovalStatus(ChangeRequestStatus.PENDING_APPROVAL);

        CrApproverEntity a1 = new CrApproverEntity();
        a1.setRequired(true);
        a1.setStatus(ApproverStatus.APPROVED);
        changeRequest.getApprovers().add(a1);

        changeRequest.evaluateApprovalClosure();

        assertEquals(ChangeRequestStatus.APPROVED, changeRequest.getStatus());
        assertEquals(ChangeRequestStatus.APPROVED, changeRequest.getApprovalStatus());
    }

    @Test
    void cancelSyncsApprovalStatus() {
        ChangeRequestEntity changeRequest = baseChangeRequest();
        changeRequest.submit();

        changeRequest.cancel();

        assertEquals(ChangeRequestStatus.CANCELLED, changeRequest.getStatus());
        assertEquals(ChangeRequestStatus.CANCELLED, changeRequest.getApprovalStatus());
    }

    // ── RW15-002: department FK fields ──────────────────────────────────────────

    @Test
    void requestDepartmentIdDefaultsToNull() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        assertNull(changeRequest.getRequestDepartmentId());
    }

    @Test
    void requestDepartmentIdCanBeSetAndRetrieved() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        UUID deptId = UUID.randomUUID();
        changeRequest.setRequestDepartmentId(deptId);
        assertEquals(deptId, changeRequest.getRequestDepartmentId());
    }

    @Test
    void destinationDepartmentIdDefaultsToNull() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        assertNull(changeRequest.getDestinationDepartmentId());
    }

    @Test
    void destinationDepartmentIdCanBeSetAndRetrieved() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        UUID deptId = UUID.randomUUID();
        changeRequest.setDestinationDepartmentId(deptId);
        assertEquals(deptId, changeRequest.getDestinationDepartmentId());
    }

    private ChangeRequestEntity baseChangeRequest() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setTitle("Upgrade core switch firmware");
        changeRequest.setPriority(Priority.HIGH);
        changeRequest.setRiskLevel(RiskLevel.MEDIUM);
        changeRequest.setApprovalType(ApprovalType.LINEAR);
        return changeRequest;
    }
}
