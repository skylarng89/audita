package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.AuditExportRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditExportRequestRepository extends JpaRepository<AuditExportRequestEntity, UUID> {
    Optional<AuditExportRequestEntity> findByDownloadToken(String downloadToken);

    List<AuditExportRequestEntity> findByStatusAndTokenExpiresAtBefore(
            AuditExportRequestEntity.Status status,
            OffsetDateTime cutoff);

    List<AuditExportRequestEntity> findByStatusInAndCompletedAtBefore(
            Collection<AuditExportRequestEntity.Status> statuses,
            OffsetDateTime cutoff);
}
