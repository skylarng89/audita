package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestUatCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequestUatCommentRepository extends JpaRepository<RequestUatCommentEntity, UUID> {
    List<RequestUatCommentEntity> findByUatIdOrderByCreatedAtDesc(UUID uatId);
}
