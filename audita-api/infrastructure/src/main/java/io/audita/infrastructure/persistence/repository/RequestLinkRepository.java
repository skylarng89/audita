package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequestLinkRepository extends JpaRepository<RequestLinkEntity, UUID> {

    List<RequestLinkEntity> findByRequestIdAOrRequestIdB(UUID idA, UUID idB);

    boolean existsByRequestIdAAndRequestIdB(UUID idA, UUID idB);
}
