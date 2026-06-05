package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query(value = "INSERT INTO idempotency_keys (user_id, operation, idempotency_key, resource_id, expires_at, created_at) "
                 + "VALUES (:userId, :operation, :key, CAST('00000000-0000-0000-0000-000000000000' AS UUID), :expiresAt, NOW()) "
                 + "ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING",
           nativeQuery = true)
    int tryClaimPlaceholder(@Param("userId") UUID userId,
                            @Param("operation") String operation,
                            @Param("key") String key,
                            @Param("expiresAt") OffsetDateTime expiresAt);

    @Modifying
    @Query(value = "UPDATE idempotency_keys SET resource_id = :resourceId "
                 + "WHERE user_id = :userId AND operation = :operation AND idempotency_key = :key "
                 + "AND resource_id = '00000000-0000-0000-0000-000000000000'",
           nativeQuery = true)
    int updateResourceId(@Param("userId") UUID userId,
                          @Param("operation") String operation,
                          @Param("key") String key,
                          @Param("resourceId") UUID resourceId);
}
