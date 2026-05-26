package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    long deleteByIsSampleTrue();
    long countByIsSampleTrue();

    List<CommentEntity> findByChangeRequestIdOrderByCreatedAtDesc(UUID changeRequestId);
}
