package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.SuperAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdminEntity, UUID> {

    Optional<SuperAdminEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long count();
}
