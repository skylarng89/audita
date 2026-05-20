package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.IdempotencyKeyEntity;
import io.audita.infrastructure.persistence.repository.IdempotencyKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class IdempotencyService {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Value("${audita.idempotency.ttl-hours:48}")
    private long ttlHours;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findResourceId(UUID userId, String operation, String rawKey) {
        String key = normalizeKey(rawKey);
        if (key == null) {
            return Optional.empty();
        }

        OffsetDateTime now = OffsetDateTime.now();
        return idempotencyKeyRepository
                .findFirstByUserIdAndOperationAndIdempotencyKeyAndExpiresAtAfter(userId, operation, key, now)
                .map(IdempotencyKeyEntity::getResourceId);
    }

    public void recordResource(UUID userId, String operation, String rawKey, UUID resourceId) {
        String key = normalizeKey(rawKey);
        if (key == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        idempotencyKeyRepository.deleteByExpiresAtBefore(now);
        try {
            idempotencyKeyRepository.save(new IdempotencyKeyEntity(
                    userId,
                    operation,
                    key,
                    resourceId,
                    now.plusHours(ttlHours)));
        } catch (DataIntegrityViolationException ignored) {
            // Duplicate key already recorded by a previous successful request.
        }
    }

    private String normalizeKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }

        String key = rawKey.trim();
        if (key.isEmpty()) {
            return null;
        }

        if (!IDEMPOTENCY_KEY_PATTERN.matcher(key).matches()) {
            throw new DomainNotPermittedException(
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency key must be 8-128 chars and contain only letters, numbers, ., _, :, or -."
            );
        }
        return key;
    }
}
