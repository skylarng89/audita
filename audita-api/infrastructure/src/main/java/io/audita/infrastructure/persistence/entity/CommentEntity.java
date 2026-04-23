package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequestEntity changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    // Sanitised HTML stored after OWASP sanitisation
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // updatedAt kept for schema compatibility; comments are immutable in v1
    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public CommentEntity(ChangeRequestEntity changeRequest, UserEntity author, String body) {
        this.changeRequest = changeRequest;
        this.author = author;
        this.body = body;
    }
}
