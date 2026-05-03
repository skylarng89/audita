package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "comment_mentions")
public class CommentMentionEntity {

    @EmbeddedId
    private CommentMentionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("commentId")
    @JoinColumn(name = "comment_id", nullable = false)
    private CommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    protected CommentMentionEntity() {}

    public CommentMentionEntity(CommentEntity comment, UserEntity user) {
        this.id = new CommentMentionId(comment.getId(), user.getId());
        this.comment = comment;
        this.user = user;
    }

    public CommentMentionId getId() {
        return id;
    }

    public CommentEntity getComment() {
        return comment;
    }

    public UserEntity getUser() {
        return user;
    }
}
