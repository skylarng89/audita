package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestUatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequestUatRepository extends JpaRepository<RequestUatEntity, UUID> {
    Optional<RequestUatEntity> findByRequestId(UUID requestId);
    boolean existsByRequestId(UUID requestId);
}
