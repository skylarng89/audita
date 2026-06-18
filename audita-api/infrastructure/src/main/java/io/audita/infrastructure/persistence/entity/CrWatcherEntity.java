package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cr_watchers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"change_request_id", "user_id"}))
public class CrWatcherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequestEntity changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "is_sample", nullable = false)
    private boolean isSample = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public CrWatcherEntity() {}

    public CrWatcherEntity(ChangeRequestEntity changeRequest, UserEntity user) {
        this.changeRequest = changeRequest;
        this.user = user;
    }

    public UUID getId() { return id; }
    public ChangeRequestEntity getChangeRequest() { return changeRequest; }
    public UserEntity getUser() { return user; }
    public boolean isSample() { return isSample; }
    public void setSample(boolean sample) { this.isSample = sample; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
