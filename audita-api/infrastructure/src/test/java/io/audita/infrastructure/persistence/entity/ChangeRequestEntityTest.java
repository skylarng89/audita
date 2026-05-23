package io.audita.infrastructure.persistence.entity;

import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ApproverStatus;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private ChangeRequestEntity baseChangeRequest() {
        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setTitle("Upgrade core switch firmware");
        changeRequest.setPriority(Priority.HIGH);
        changeRequest.setRiskLevel(RiskLevel.MEDIUM);
        changeRequest.setApprovalType(ApprovalType.LINEAR);
        return changeRequest;
    }
}
