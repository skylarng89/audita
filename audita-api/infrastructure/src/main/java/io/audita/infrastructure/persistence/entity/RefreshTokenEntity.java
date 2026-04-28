package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "user_agent_hash")
    private String userAgentHash;

    @Column(name = "ip_hash")
    private String ipHash;

    @Column(nullable = false)
    private boolean revoked = false;

    protected RefreshTokenEntity() {}

    public RefreshTokenEntity(UserEntity user, String tokenHash, OffsetDateTime expiresAt) {
        this(user, tokenHash, expiresAt, null, null);
    }

    public RefreshTokenEntity(UserEntity user,
                              String tokenHash,
                              OffsetDateTime expiresAt,
                              String userAgentHash,
                              String ipHash) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.userAgentHash = userAgentHash;
        this.ipHash = ipHash;
    }

    public boolean isExpired() { return OffsetDateTime.now().isAfter(expiresAt); }
    public boolean isValid() { return !revoked && !isExpired(); }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public String getUserAgentHash() { return userAgentHash; }
    public String getIpHash() { return ipHash; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
