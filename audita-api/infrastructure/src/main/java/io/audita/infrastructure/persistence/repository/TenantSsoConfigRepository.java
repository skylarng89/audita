package io.audita.infrastructure.persistence.repository;

import io.audita.domain.model.OAuthProvider;
import io.audita.infrastructure.persistence.entity.TenantSsoConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantSsoConfigRepository extends JpaRepository<TenantSsoConfigEntity, UUID> {

    @Query("SELECT c FROM TenantSsoConfigEntity c WHERE c.tenant.slug = :slug AND c.provider = :provider AND c.enabled = true")
    Optional<TenantSsoConfigEntity> findActiveByTenantSlugAndProvider(
            @Param("slug") String slug,
            @Param("provider") OAuthProvider provider);

    @Query("SELECT c FROM TenantSsoConfigEntity c WHERE c.tenant.id = :tenantId")
    List<TenantSsoConfigEntity> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT c FROM TenantSsoConfigEntity c WHERE c.tenant.id = :tenantId AND c.provider = :provider")
    Optional<TenantSsoConfigEntity> findByTenantIdAndProvider(
            @Param("tenantId") UUID tenantId,
            @Param("provider") OAuthProvider provider);
}
