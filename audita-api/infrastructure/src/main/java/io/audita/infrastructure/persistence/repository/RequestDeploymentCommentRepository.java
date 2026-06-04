package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestDeploymentCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequestDeploymentCommentRepository extends JpaRepository<RequestDeploymentCommentEntity, UUID> {
    List<RequestDeploymentCommentEntity> findByDeploymentIdOrderByCreatedAtDesc(UUID deploymentId);
}
