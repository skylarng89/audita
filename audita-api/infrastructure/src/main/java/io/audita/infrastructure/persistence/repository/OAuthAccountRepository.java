package io.audita.infrastructure.persistence.repository;

import io.audita.domain.model.OAuthProvider;
import io.audita.infrastructure.persistence.entity.OAuthAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, UUID> {

    Optional<OAuthAccountEntity> findByProviderAndProviderSub(OAuthProvider provider, String providerSub);
}
