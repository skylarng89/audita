package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequestDeploymentRepository extends JpaRepository<RequestDeploymentEntity, UUID> {
    Optional<RequestDeploymentEntity> findByRequestId(UUID requestId);
    Optional<RequestDeploymentEntity> findByUatId(UUID uatId);
    boolean existsByRequestId(UUID requestId);
}
