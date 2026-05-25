package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, UUID> {

    long deleteByIsSampleTrue();
    long countByIsSampleTrue();

    List<AttachmentEntity> findByChangeRequestIdOrderByCreatedAtDesc(UUID changeRequestId);

    Optional<AttachmentEntity> findByIdAndChangeRequestId(UUID id, UUID changeRequestId);
}
