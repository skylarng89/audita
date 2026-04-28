package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.CommentMentionEntity;
import io.audita.infrastructure.persistence.entity.CommentMentionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentMentionRepository extends JpaRepository<CommentMentionEntity, CommentMentionId> {
}
