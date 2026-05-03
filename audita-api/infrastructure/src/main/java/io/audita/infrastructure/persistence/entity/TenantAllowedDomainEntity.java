package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_allowed_domains", schema = "public")
public class TenantAllowedDomainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected TenantAllowedDomainEntity() {}

    public TenantAllowedDomainEntity(TenantEntity tenant, String domain) {
        this.tenant = tenant;
        this.domain = domain;
    }

    public UUID getId() { return id; }
    public TenantEntity getTenant() { return tenant; }
    public String getDomain() { return domain; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
