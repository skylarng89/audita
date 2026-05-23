package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrApproverRepository extends JpaRepository<CrApproverEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<CrApproverEntity> findWithUserAndRolesById(UUID id);

    @EntityGraph(attributePaths = {"user", "user.roles"})
    List<CrApproverEntity> findByChangeRequestIdOrderByPositionAsc(UUID changeRequestId);

    Optional<CrApproverEntity> findByChangeRequestIdAndUserId(UUID changeRequestId, UUID userId);

    int countByChangeRequestId(UUID changeRequestId);
}
