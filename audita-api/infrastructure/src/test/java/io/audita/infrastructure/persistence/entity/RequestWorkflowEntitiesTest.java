package io.audita.infrastructure.persistence.entity;

import io.audita.domain.exception.DomainException;
import io.audita.domain.model.ApproverStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestWorkflowEntitiesTest {

    // ── RequestUatEntity defaults ──────────────────────────────────────────────

    @Test
    void uatEntityDefaultsStatusToInProgress() {
        RequestUatEntity uat = new RequestUatEntity();
        assertEquals("IN_PROGRESS", uat.getStatus());
    }

    @Test
    void uatEntityDefaultsReadOnlyToFalse() {
        RequestUatEntity uat = new RequestUatEntity();
        assertFalse(uat.isReadOnly());
    }

    @Test
    void uatEntityFieldsCanBeSet() {
        RequestUatEntity uat = new RequestUatEntity();
        UUID requestId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        uat.setRequestId(requestId);
        uat.setTitle("UAT for CR-001");
        uat.setDetails("Verify firmware upgrade");
        uat.setCreatedBy(createdBy);

        assertEquals(requestId, uat.getRequestId());
        assertEquals("UAT for CR-001", uat.getTitle());
        assertEquals("Verify firmware upgrade", uat.getDetails());
        assertEquals(createdBy, uat.getCreatedBy());
    }

    // ── RequestDeploymentEntity defaults ────────────────────────────────────────

    @Test
    void deploymentEntityDefaultsStatusToPending() {
        RequestDeploymentEntity deployment = new RequestDeploymentEntity();
        assertEquals("PENDING", deployment.getStatus());
    }

    @Test
    void deploymentEntityFieldsCanBeSet() {
        RequestDeploymentEntity deployment = new RequestDeploymentEntity();
        UUID requestId = UUID.randomUUID();
        UUID uatId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        deployment.setRequestId(requestId);
        deployment.setUatId(uatId);
        deployment.setCreatedBy(createdBy);

        assertEquals(requestId, deployment.getRequestId());
        assertEquals(uatId, deployment.getUatId());
        assertEquals(createdBy, deployment.getCreatedBy());
    }

    // ── RequestUatApproverEntity domain methods ─────────────────────────────────

    @Test
    void uatApproverDefaultsToPending() {
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        assertEquals(ApproverStatus.PENDING, approver.getStatus());
    }

    @Test
    void uatApproverApproveSetsStatusAndDecidedAt() {
        RequestUatApproverEntity approver = new RequestUatApproverEntity();

        approver.approve();

        assertEquals(ApproverStatus.APPROVED, approver.getStatus());
        assertNotNull(approver.getDecidedAt());
        assertNull(approver.getRejectionReason());
    }

    @Test
    void uatApproverRejectSetsStatusReasonAndDecidedAt() {
        RequestUatApproverEntity approver = new RequestUatApproverEntity();

        approver.reject("Tests failing on staging");

        assertEquals(ApproverStatus.REJECTED, approver.getStatus());
        assertEquals("Tests failing on staging", approver.getRejectionReason());
        assertNotNull(approver.getDecidedAt());
    }

    @Test
    void uatApproverRejectThrowsOnBlankReason() {
        RequestUatApproverEntity approver = new RequestUatApproverEntity();
        assertThrows(DomainException.class, () -> approver.reject(""));
        assertThrows(DomainException.class, () -> approver.reject(null));
    }

}

