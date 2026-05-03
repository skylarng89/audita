package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.TenantAllowedDomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TenantAllowedDomainRepository extends JpaRepository<TenantAllowedDomainEntity, UUID> {

    @Query("SELECT d FROM TenantAllowedDomainEntity d WHERE d.tenant.slug = :slug")
    List<TenantAllowedDomainEntity> findByTenantSlug(@Param("slug") String slug);
}
