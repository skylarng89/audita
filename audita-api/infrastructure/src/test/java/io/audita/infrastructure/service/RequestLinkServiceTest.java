package io.audita.infrastructure.service;

import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RiskLevel;
import io.audita.domain.model.ApprovalType;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestLinkEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestLinkRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLinkServiceTest {

    @Mock RequestLinkRepository requestLinkRepository;
    @Mock ChangeRequestRepository changeRequestRepository;
    @Mock UserRepository userRepository;
    @Mock ActivityStreamRepository activityStreamRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks
    RequestLinkService requestLinkService;

    @Test
    void searchRequests_returns_matching_by_displayId() {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", UUID.randomUUID());
        cr.setDisplayId("CR-001");
        cr.setTitle("Database migration");
        cr.setStatus(ChangeRequestStatus.DRAFT);

        when(changeRequestRepository.searchByDisplayIdOrTitle(eq("CR-001"), any(PageRequest.class)))
                .thenReturn(List.of(cr));

        List<ChangeRequestEntity> results = requestLinkService.searchRequests("CR-001", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDisplayId()).isEqualTo("CR-001");
    }

    @Test
    void searchRequests_returns_matching_by_title() {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", UUID.randomUUID());
        cr.setDisplayId("CR-002");
        cr.setTitle("Network upgrade");
        cr.setStatus(ChangeRequestStatus.PENDING_APPROVAL);

        when(changeRequestRepository.searchByDisplayIdOrTitle(eq("network"), any(PageRequest.class)))
                .thenReturn(List.of(cr));

        List<ChangeRequestEntity> results = requestLinkService.searchRequests("network", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Network upgrade");
    }

    @Test
    void getLinkedRequests_returns_both_directions() {
        UUID requestId = UUID.randomUUID();
        UUID otherId1 = UUID.randomUUID();
        UUID otherId2 = UUID.randomUUID();

        RequestLinkEntity link1 = new RequestLinkEntity();
        link1.setRequestIdA(requestId);
        link1.setRequestIdB(otherId1);

        RequestLinkEntity link2 = new RequestLinkEntity();
        link2.setRequestIdA(otherId2);
        link2.setRequestIdB(requestId);

        when(requestLinkRepository.findByRequestIdAOrRequestIdB(requestId, requestId))
                .thenReturn(List.of(link1, link2));

        List<UUID> linked = requestLinkService.getLinkedRequests(requestId);

        assertThat(linked).containsExactlyInAnyOrder(otherId1, otherId2);
    }

    @Test
    void upsertLinks_adds_new_links() {
        UUID requestId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setTitle("Source CR");

        UserEntity actor = new UserEntity("actor@example.com", "Actor User");
        ReflectionTestUtils.setField(actor, "id", actorId);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(changeRequestRepository.findById(targetId)).thenReturn(Optional.of(new ChangeRequestEntity()));
        when(requestLinkRepository.findByRequestIdAOrRequestIdB(requestId, requestId))
                .thenReturn(List.of());

        requestLinkService.upsertLinks(requestId, List.of(targetId), actorId);

        ArgumentCaptor<RequestLinkEntity> captor = ArgumentCaptor.forClass(RequestLinkEntity.class);
        verify(requestLinkRepository).save(captor.capture());
        RequestLinkEntity saved = captor.getValue();
        assertThat(saved.getLinkedBy()).isEqualTo(actorId);
    }

    @Test
    void upsertLinks_removes_old_links_not_in_new_set() {
        UUID requestId = UUID.randomUUID();
        UUID oldTargetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setTitle("Source CR");

        UserEntity actor = new UserEntity("actor@example.com", "Actor User");
        ReflectionTestUtils.setField(actor, "id", actorId);

        RequestLinkEntity oldLink = new RequestLinkEntity();
        ReflectionTestUtils.setField(oldLink, "id", UUID.randomUUID());
        UUID[] canonical = RequestLinkEntity.canonicalOrder(requestId, oldTargetId);
        oldLink.setRequestIdA(canonical[0]);
        oldLink.setRequestIdB(canonical[1]);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(requestLinkRepository.findByRequestIdAOrRequestIdB(requestId, requestId))
                .thenReturn(List.of(oldLink));

        requestLinkService.upsertLinks(requestId, List.of(), actorId);

        verify(requestLinkRepository).delete(oldLink);
        verify(requestLinkRepository, never()).save(any());
    }

    @Test
    void upsertLinks_rejects_self_link() {
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        assertThatThrownBy(() -> requestLinkService.upsertLinks(requestId, List.of(requestId), actorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    void upsertLinks_uses_canonical_ordering() {
        UUID requestId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setTitle("Source CR");

        UserEntity actor = new UserEntity("actor@example.com", "Actor User");
        ReflectionTestUtils.setField(actor, "id", actorId);

        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(changeRequestRepository.findById(targetId)).thenReturn(Optional.of(new ChangeRequestEntity()));
        when(requestLinkRepository.findByRequestIdAOrRequestIdB(requestId, requestId))
                .thenReturn(List.of());

        requestLinkService.upsertLinks(requestId, List.of(targetId), actorId);

        ArgumentCaptor<RequestLinkEntity> captor = ArgumentCaptor.forClass(RequestLinkEntity.class);
        verify(requestLinkRepository).save(captor.capture());
        RequestLinkEntity saved = captor.getValue();

        UUID[] expected = RequestLinkEntity.canonicalOrder(requestId, targetId);
        assertThat(saved.getRequestIdA()).isEqualTo(expected[0]);
        assertThat(saved.getRequestIdB()).isEqualTo(expected[1]);
    }
}
