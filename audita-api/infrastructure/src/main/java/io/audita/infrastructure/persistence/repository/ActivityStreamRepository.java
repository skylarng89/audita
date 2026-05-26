package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityStreamRepository extends JpaRepository<ActivityStreamEntity, UUID> {

    long deleteByIsSampleTrue();
    long countByIsSampleTrue();

    List<ActivityStreamEntity> findByChangeRequestIdOrderByCreatedAtDesc(UUID changeRequestId);
}
