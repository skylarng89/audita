package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.CommentMentionEntity;
import io.audita.infrastructure.persistence.entity.CommentMentionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentMentionRepository extends JpaRepository<CommentMentionEntity, CommentMentionId> {

    @Modifying
    @Query("DELETE FROM CommentMentionEntity cm WHERE cm.isSample = true")
    long deleteByIsSampleTrue();

    @Query("SELECT COUNT(cm) FROM CommentMentionEntity cm WHERE cm.isSample = true")
    long countByIsSampleTrue();
}
