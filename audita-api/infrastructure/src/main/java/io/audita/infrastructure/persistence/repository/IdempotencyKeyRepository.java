package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity> findFirstByUserIdAndOperationAndIdempotencyKeyAndExpiresAtAfter(
            UUID userId,
            String operation,
            String idempotencyKey,
            OffsetDateTime now);

    void deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
