package io.audita.infrastructure.persistence.entity;

import io.audita.domain.model.OAuthProvider;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_accounts")
public class OAuthAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    // The stable subject identifier from the OAuth provider (e.g. Google `sub` claim)
    @Column(nullable = false)
    private String providerSub;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime linkedAt = OffsetDateTime.now();

    protected OAuthAccountEntity() {}

    public OAuthAccountEntity(UserEntity user, OAuthProvider provider,
                               String providerSub, String email) {
        this.user = user;
        this.provider = provider;
        this.providerSub = providerSub;
        this.email = email;
    }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public OAuthProvider getProvider() { return provider; }
    public String getProviderSub() { return providerSub; }
    public String getEmail() { return email; }
    public OffsetDateTime getLinkedAt() { return linkedAt; }
}
