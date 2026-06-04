package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestUatApproverRepository extends JpaRepository<RequestUatApproverEntity, UUID> {
    List<RequestUatApproverEntity> findByUatIdOrderByPositionAsc(UUID uatId);
    Optional<RequestUatApproverEntity> findByUatIdAndUserId(UUID uatId, UUID userId);
    int countByUatId(UUID uatId);
}
