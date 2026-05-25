package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.GroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, UUID> {

    long deleteByIsSampleTrue();
    long countByIsSampleTrue();

    boolean existsByName(String name);

    Optional<GroupEntity> findByName(String name);

    Page<GroupEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
