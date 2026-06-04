package io.audita.infrastructure.service;

import io.audita.domain.exception.NotFoundException;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.RequestLinkEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.RequestLinkRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class RequestLinkService {

    private static final String ENTITY_CHANGE_REQUEST = "change_request";

    private final RequestLinkRepository requestLinkRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final AuditLogService auditLogService;

    public RequestLinkService(RequestLinkRepository requestLinkRepository,
                              ChangeRequestRepository changeRequestRepository,
                              UserRepository userRepository,
                              ActivityStreamRepository activityStreamRepository,
                              AuditLogService auditLogService) {
        this.requestLinkRepository = requestLinkRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.userRepository = userRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.auditLogService = auditLogService;
    }

    public List<ChangeRequestEntity> searchRequests(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return changeRequestRepository.searchByDisplayIdOrTitle(query, PageRequest.of(0, limit));
    }

    public List<UUID> getLinkedRequests(UUID requestId) {
        List<RequestLinkEntity> links = requestLinkRepository.findByRequestIdAOrRequestIdB(requestId, requestId);
        return links.stream()
                .map(link -> link.getRequestIdA().equals(requestId) ? link.getRequestIdB() : link.getRequestIdA())
                .toList();
    }

    public void upsertLinks(UUID requestId, List<UUID> linkedRequestIds, UUID actorUserId) {
        if (linkedRequestIds.contains(requestId)) {
            throw new IllegalArgumentException("Cannot link a request to itself");
        }

        ChangeRequestEntity sourceRequest = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("ChangeRequest", requestId));
        UserEntity actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new NotFoundException("User", actorUserId));

        List<RequestLinkEntity> existingLinks = requestLinkRepository.findByRequestIdAOrRequestIdB(requestId, requestId);
        Set<UUID> existingTargetIds = new HashSet<>();
        for (RequestLinkEntity link : existingLinks) {
            UUID targetId = link.getRequestIdA().equals(requestId) ? link.getRequestIdB() : link.getRequestIdA();
            existingTargetIds.add(targetId);
        }

        Set<UUID> newTargetIds = new HashSet<>(linkedRequestIds);

        List<UUID> toRemove = new ArrayList<>(existingTargetIds);
        toRemove.removeAll(newTargetIds);

        List<UUID> toAdd = new ArrayList<>(newTargetIds);
        toAdd.removeAll(existingTargetIds);

        for (RequestLinkEntity link : existingLinks) {
            UUID targetId = link.getRequestIdA().equals(requestId) ? link.getRequestIdB() : link.getRequestIdA();
            if (toRemove.contains(targetId)) {
                requestLinkRepository.delete(link);
            }
        }

        for (UUID targetId : toAdd) {
            changeRequestRepository.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("ChangeRequest", targetId));

            UUID[] canonical = RequestLinkEntity.canonicalOrder(requestId, targetId);
            RequestLinkEntity newLink = new RequestLinkEntity();
            newLink.setRequestIdA(canonical[0]);
            newLink.setRequestIdB(canonical[1]);
            newLink.setLinkedBy(actorUserId);
            requestLinkRepository.save(newLink);
        }

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
            logActivity(sourceRequest, actor, "CR_LINKS_UPDATED", Map.of(
                    "added", toAdd,
                    "removed", toRemove
            ));
            auditLogService.log("CR_LINKS_UPDATED", ENTITY_CHANGE_REQUEST, requestId,
                    actorUserId, actor.getEmail(), Map.of(
                            "added", toAdd,
                            "removed", toRemove
                    ), null);
        }
    }

    private void logActivity(ChangeRequestEntity changeRequest, UserEntity actor,
                             String actionType, Map<String, Object> payload) {
        activityStreamRepository.save(new ActivityStreamEntity(changeRequest, actor, actionType, payload));
    }
}
