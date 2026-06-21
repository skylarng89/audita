package io.audita.infrastructure.service;

import io.audita.application.port.AuditTrailPort;
import io.audita.infrastructure.persistence.entity.AuditLogEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.AuditLogRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles writing and querying the global immutable audit log.
 * Write path is intentionally simple — never throws, so callers are never
 * blocked by audit-log persistence failures.
 *
 * Each entry carries a SHA-256 hash linking it to its predecessor, forming a
 * cryptographically tamper-evident chain. Altering any record invalidates all
 * subsequent hashes, making tampering detectable even by a database
 * administrator with superuser access.
 */
@Service
public class AuditLogService implements AuditTrailPort {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists a single audit log entry with cryptographic hash chaining.
     * Uses REQUIRES_NEW so the audit entry is committed independently of
     * the caller's transaction, surviving any rollback in the business
     * operation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actionType,
                    String entityType,
                    UUID entityId,
                    UUID actorId,
                    String actorEmail,
                    Map<String, Object> payload,
                    String ipAddress) {
        try {
            long chainIndex = auditLogRepository.getMaxChainIndex() + 1;
            byte[] previousHash = auditLogRepository.findLastRecordHash();

            byte[] recordHash = computeRecordHash(
                    actorId, actorEmail, actionType, entityType, entityId,
                    payload, ipAddress, previousHash);

            auditLogRepository.save(new AuditLogEntity(
                    actorId, actorEmail, actionType, entityType, entityId,
                    payload, ipAddress, chainIndex, recordHash, previousHash));
        } catch (Exception e) {
            log.error("Failed to write audit log entry: actionType={} entityType={}", actionType, entityType, e);
        }
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

    @Override
    @Transactional(readOnly = true)
    public IntegrityResult verifyIntegrity() {
        List<AuditLogEntity> entries = auditLogRepository.findAllByOrderByChainIndexAsc();
        if (entries.isEmpty()) {
            return new IntegrityResult(false, 0, 0, List.of());
        }

        List<UUID> tampered = new ArrayList<>();
        long verifiableCount = 0;
        byte[] expectedPrevious = null;

        for (AuditLogEntity entry : entries) {
            if (entry.getChainIndex() == null) {
                continue;
            }
            if (entry.getRecordHash() == null) {
                continue;
            }

            verifiableCount++;

            byte[] recomputed = computeRecordHash(
                    entry.getActorId(), entry.getActorEmail(),
                    entry.getActionType(), entry.getEntityType(),
                    entry.getEntityId(), entry.getPayload(),
                    entry.getIpAddress(), entry.getPreviousHash());

            if (!MessageDigest.isEqual(recomputed, entry.getRecordHash())) {
                tampered.add(entry.getId());
            }

            if (entry.getChainIndex() > 1) {
                if (entry.getPreviousHash() == null) {
                    tampered.add(entry.getId());
                    continue;
                }
                if (expectedPrevious != null && !MessageDigest.isEqual(entry.getPreviousHash(), expectedPrevious)) {
                    if (!tampered.contains(entry.getId())) {
                        tampered.add(entry.getId());
                    }
                }
            }

            expectedPrevious = entry.getRecordHash();
        }

        boolean tamperedFlag = !tampered.isEmpty();
        if (tamperedFlag) {
            log.warn("Audit log tampering detected: tamperedRecords={} totalRecords={} verifiableRecords={}",
                    tampered.size(), entries.size(), verifiableCount);
        } else {
            log.info("Audit log integrity verified: totalRecords={} verifiableRecords={}",
                    entries.size(), verifiableCount);
        }

        return new IntegrityResult(
                tamperedFlag,
                entries.size(),
                verifiableCount,
                tampered);
    }

    // ── Hash computation ──────────────────────────────────────────────────────

    private byte[] computeRecordHash(UUID actorId, String actorEmail,
                                     String actionType, String entityType,
                                     UUID entityId, Map<String, Object> payload,
                                     String ipAddress, byte[] previousHash) {
        String canonical = canonicalField(actorId) + "|"
                + canonicalField(actorEmail) + "|"
                + canonicalField(actionType) + "|"
                + canonicalField(entityType) + "|"
                + canonicalField(entityId) + "|"
                + canonicalPayload(payload) + "|"
                + canonicalField(ipAddress) + "|"
                + hexOrEmpty(previousHash);
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String canonicalField(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private static String canonicalPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        return canonicalJsonValue(new TreeMap<>(payload));
    }

    @SuppressWarnings("unchecked")
    private static String canonicalJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(entry.getKey().toString(), (Object) entry.getValue());
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(canonicalJsonValue(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(canonicalJsonValue(item));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String hexOrEmpty(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return HexFormat.of().formatHex(bytes);
    }

    // ── Query helpers ─────────────────────────────────────────────────────────

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
        UUID actorId = e.getActorId();
        return new AuditLogEntry(
                e.getId(),
                actorId,
                actorId != null ? actorNamesById.get(actorId) : null,
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
