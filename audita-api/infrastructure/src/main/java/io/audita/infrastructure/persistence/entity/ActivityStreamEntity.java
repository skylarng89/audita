package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "activity_stream")
@Getter
@Setter
@NoArgsConstructor
public class ActivityStreamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequestEntity changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    @Column(nullable = false)
    private String actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ActivityStreamEntity(ChangeRequestEntity changeRequest, UserEntity actor,
                                 String actionType, Map<String, Object> payload) {
        this.changeRequest = changeRequest;
        this.actor = actor;
        this.actionType = actionType;
        this.payload = payload;
    }
}
