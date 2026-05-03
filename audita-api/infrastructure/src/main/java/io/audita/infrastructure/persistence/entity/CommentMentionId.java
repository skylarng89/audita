package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CommentMentionId implements Serializable {

    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected CommentMentionId() {}

    public CommentMentionId(UUID commentId, UUID userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommentMentionId that)) {
            return false;
        }
        return Objects.equals(commentId, that.commentId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentId, userId);
    }
}
