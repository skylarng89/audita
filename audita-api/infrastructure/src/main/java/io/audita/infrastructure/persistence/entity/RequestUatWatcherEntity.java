package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "request_uat_watchers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"uat_id", "user_id"}))
public class RequestUatWatcherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uat_id", nullable = false)
    private RequestUatEntity uat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "is_sample", nullable = false)
    private boolean sample = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public RequestUatWatcherEntity() {}

    public RequestUatWatcherEntity(RequestUatEntity uat, UserEntity user) {
        this.uat = uat;
        this.user = user;
    }

    public UUID getId() { return id; }
    public RequestUatEntity getUat() { return uat; }
    public UserEntity getUser() { return user; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
