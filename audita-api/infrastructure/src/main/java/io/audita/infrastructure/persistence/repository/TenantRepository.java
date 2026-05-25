package io.audita.infrastructure.persistence.repository;

import io.audita.domain.model.TenantStatus;
import io.audita.infrastructure.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findBySlug(String slug);

    Optional<TenantEntity> findBySubdomain(String subdomain);

    boolean existsBySlug(String slug);

    boolean existsBySubdomain(String subdomain);

    Optional<TenantEntity> findFirstByOrderByCreatedAtAsc();

    List<TenantEntity> findByStatus(TenantStatus status);
}
