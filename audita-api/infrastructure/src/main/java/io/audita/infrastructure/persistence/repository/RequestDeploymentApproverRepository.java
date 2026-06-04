package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestDeploymentApproverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestDeploymentApproverRepository extends JpaRepository<RequestDeploymentApproverEntity, UUID> {
    List<RequestDeploymentApproverEntity> findByDeploymentIdOrderByPositionAsc(UUID deploymentId);
    Optional<RequestDeploymentApproverEntity> findByDeploymentIdAndUserId(UUID deploymentId, UUID userId);
}
