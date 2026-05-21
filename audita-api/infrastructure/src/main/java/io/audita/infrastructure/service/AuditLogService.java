package io.audita.infrastructure.service;

import io.audita.application.port.AuditTrailPort;
import io.audita.infrastructure.persistence.entity.AuditLogEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.AuditLogRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles writing and querying the global immutable audit log.
 * Write path is intentionally simple — never throws, so callers are never
 * blocked by audit-log persistence failures.
 */
@Service
public class AuditLogService implements AuditTrailPort {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists a single audit log entry. Silently swallows persistence
     * errors so that a failing audit write never rolls back the caller's
     * transaction — the caller must already be within a transaction.
     */
    @Transactional
    public void log(String actionType,
                    String entityType,
                    UUID entityId,
                    UUID actorId,
                    String actorEmail,
                    Map<String, Object> payload,
                    String ipAddress) {
        auditLogRepository.save(
                new AuditLogEntity(actorId, actorEmail, actionType, entityType, entityId, payload, ipAddress)
        );
    }

    // ── AuditTrailPort ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogEntry> query(String actorEmail,
                                     String actionType,
                                     String entityType,
                                     LocalDate from,
                                     LocalDate to,
                                     Pageable pageable) {
        Specification<AuditLogEntity> spec = buildSpec(actorEmail, actionType, entityType, from, to);
        Page<AuditLogEntity> page = auditLogRepository.findAll(spec, pageable);
        Map<UUID, String> actorNamesById = resolveActorNames(page.getContent());
        return page.map(entity -> toEntry(entity, actorNamesById));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntry> export(String actorEmail,
                                      String actionType,
                                      String entityType,
                                      LocalDate from,
                                      LocalDate to) {
        Specification<AuditLogEntity> spec = buildSpec(actorEmail, actionType, entityType, from, to);
        List<AuditLogEntity> entities = auditLogRepository.findAll(spec);
        Map<UUID, String> actorNamesById = resolveActorNames(entities);
        return entities.stream()
                .map(entity -> toEntry(entity, actorNamesById))
                .toList();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Specification<AuditLogEntity> buildSpec(String actorEmail,
                                                      String actionType,
                                                      String entityType,
                                                      LocalDate from,
                                                      LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorEmail != null && !actorEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("actorEmail")),
                        "%" + actorEmail.trim().toLowerCase() + "%"));
            }
            if (actionType != null && !actionType.isBlank()) {
                predicates.add(cb.equal(root.get("actionType"), actionType.trim().toUpperCase()));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim().toLowerCase()));
            }
            if (from != null) {
                OffsetDateTime fromDt = from.atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            }
            if (to != null) {
                OffsetDateTime toDt = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.lessThan(root.get("createdAt"), toDt));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Map<UUID, String> resolveActorNames(List<AuditLogEntity> entries) {
        Set<UUID> actorIds = entries.stream()
                .map(AuditLogEntity::getActorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (actorIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByIdInAndStatusOrderByFullNameAsc(new java.util.ArrayList<>(actorIds), io.audita.domain.model.UserStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName, (left, _) -> left));
    }

    private AuditLogEntry toEntry(AuditLogEntity e, Map<UUID, String> actorNamesById) {
        return new AuditLogEntry(
                e.getId(),
                e.getActorId(),
                actorNamesById.get(e.getActorId()),
                e.getActorEmail(),
                e.getActionType(),
                e.getEntityType(),
                e.getEntityId(),
                e.getPayload(),
                e.getIpAddress(),
                e.getCreatedAt()
        );
    }
}
