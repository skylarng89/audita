package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.AttachmentRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestCustomFieldRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceSecurityTest {

    @Mock ChangeRequestRepository changeRequestRepository;
    @Mock CrApproverRepository crApproverRepository;
    @Mock ChangeRequestCustomFieldRepository customFieldRepository;
    @Mock ActivityStreamRepository activityStreamRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    ChangeRequestService changeRequestService;

    @Test
    void update_denies_requester_who_does_not_own_change_request() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherRequesterId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));

        assertThatThrownBy(() -> changeRequestService.update(
                changeRequestId,
                "Updated by attacker",
                "should fail",
                Priority.HIGH,
                RiskLevel.HIGH,
                "Security",
                ApprovalType.LINEAR,
                OffsetDateTime.now().plusHours(2),
                OffsetDateTime.now().plusHours(3),
                new String[] {"db"},
                otherRequesterId,
                "REQUESTER"
        ))
                .isInstanceOf(DomainNotPermittedException.class)
                .extracting(ex -> ((DomainNotPermittedException) ex).getErrorCode())
                .isEqualTo("FORBIDDEN");

        verifyNoInteractions(activityStreamRepository);
        verify(changeRequestRepository).findById(changeRequestId);
    }

    @Test
    void submit_denies_requester_who_does_not_own_change_request() {
        UUID changeRequestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherRequesterId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = buildDraftChangeRequest(ownerId);
        when(changeRequestRepository.findById(changeRequestId)).thenReturn(Optional.of(changeRequest));

        assertThatThrownBy(() -> changeRequestService.submit(changeRequestId, otherRequesterId, "REQUESTER"))
                .isInstanceOf(DomainNotPermittedException.class)
                .extracting(ex -> ((DomainNotPermittedException) ex).getErrorCode())
                .isEqualTo("FORBIDDEN");

        verifyNoInteractions(activityStreamRepository);
        verify(changeRequestRepository).findById(changeRequestId);
    }

    private ChangeRequestEntity buildDraftChangeRequest(UUID ownerId) {
        UserEntity owner = new UserEntity("owner@example.com", "Owner User");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        changeRequest.setTitle("Baseline CR");
        changeRequest.setDescription("baseline");
        changeRequest.setPriority(Priority.MEDIUM);
        changeRequest.setRiskLevel(RiskLevel.MEDIUM);
        changeRequest.setApprovalType(ApprovalType.LINEAR);
        changeRequest.setCreatedBy(owner);
        return changeRequest;
    }
}
