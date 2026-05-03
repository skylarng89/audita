package io.audita.infrastructure.persistence.entity;

import io.audita.domain.model.OAuthProvider;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_sso_configs", schema = "public")
public class TenantSsoConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(nullable = false)
    private String clientId;

    // AES-256 encrypted — decrypt via AesEncryptionService before use
    @Column(nullable = false)
    private String clientSecret;

    // Azure AD tenant ID (required for Microsoft, null for Google)
    private String msTenantId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected TenantSsoConfigEntity() {}

    public TenantSsoConfigEntity(TenantEntity tenant, OAuthProvider provider,
                                  String clientId, String clientSecret, String msTenantId) {
        this.tenant = tenant;
        this.provider = provider;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.msTenantId = msTenantId;
    }

    public UUID getId() { return id; }
    public TenantEntity getTenant() { return tenant; }
    public OAuthProvider getProvider() { return provider; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getMsTenantId() { return msTenantId; }
    public void setMsTenantId(String msTenantId) { this.msTenantId = msTenantId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
