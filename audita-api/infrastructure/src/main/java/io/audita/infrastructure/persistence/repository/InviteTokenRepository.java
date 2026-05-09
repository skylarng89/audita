package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.InviteTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InviteTokenRepository extends JpaRepository<InviteTokenEntity, UUID> {

    Optional<InviteTokenEntity> findByTokenHash(String tokenHash);

    List<InviteTokenEntity> findAllByUser_Id(UUID userId);
}
