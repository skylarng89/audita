package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestUatWatcherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestUatWatcherRepository extends JpaRepository<RequestUatWatcherEntity, UUID> {

    List<RequestUatWatcherEntity> findByUatId(UUID uatId);

    @Query("SELECT w FROM RequestUatWatcherEntity w WHERE w.uat.id = :uatId AND w.user.id = :userId")
    Optional<RequestUatWatcherEntity> findByUatIdAndUserId(UUID uatId, UUID userId);

    void deleteByUatId(UUID uatId);

    boolean existsByUatIdAndUserId(UUID uatId, UUID userId);

    void deleteByIsSampleTrue();

    long countByIsSampleTrue();
}
