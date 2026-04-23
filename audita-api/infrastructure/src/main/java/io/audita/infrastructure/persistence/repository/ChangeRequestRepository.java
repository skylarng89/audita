package io.audita.infrastructure.persistence.repository;

import io.audita.domain.model.ChangeRequestStatus;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequestEntity, UUID> {

    Page<ChangeRequestEntity> findByStatus(ChangeRequestStatus status, Pageable pageable);

    Page<ChangeRequestEntity> findByCreatedById(UUID userId, Pageable pageable);

    // CRs pending SLA breach evaluation: deadline passed and not yet flagged
    @Query("SELECT cr FROM ChangeRequestEntity cr WHERE cr.slaDeadline IS NOT NULL " +
           "AND cr.slaDeadline < :now AND cr.slaBreached = FALSE " +
           "AND cr.status = 'PENDING_APPROVAL'")
    List<ChangeRequestEntity> findSlaBreached(@Param("now") OffsetDateTime now);

    // CRs approaching SLA warning threshold
    @Query("SELECT cr FROM ChangeRequestEntity cr WHERE cr.slaDeadline IS NOT NULL " +
           "AND cr.slaDeadline BETWEEN :now AND :warningAt " +
           "AND cr.slaBreached = FALSE AND cr.status = 'PENDING_APPROVAL'")
    List<ChangeRequestEntity> findSlaWarning(
            @Param("now") OffsetDateTime now,
            @Param("warningAt") OffsetDateTime warningAt);
}
