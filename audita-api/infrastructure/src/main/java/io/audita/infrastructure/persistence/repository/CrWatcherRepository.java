package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.CrWatcherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrWatcherRepository extends JpaRepository<CrWatcherEntity, UUID> {

    List<CrWatcherEntity> findByChangeRequestId(UUID changeRequestId);

    @Query("SELECT w FROM CrWatcherEntity w WHERE w.changeRequest.id = :crId AND w.user.id = :userId")
    Optional<CrWatcherEntity> findByCrIdAndUserId(UUID crId, UUID userId);

    void deleteByChangeRequestId(UUID changeRequestId);

    boolean existsByChangeRequestIdAndUserId(UUID changeRequestId, UUID userId);

    void deleteByIsSampleTrue();

    long countByIsSampleTrue();
}
