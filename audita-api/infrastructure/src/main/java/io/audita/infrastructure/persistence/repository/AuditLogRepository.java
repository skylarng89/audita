package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.AuditLogEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>,
        JpaSpecificationExecutor<AuditLogEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditLogEntity> findTopByOrderByChainIndexDesc();

    List<AuditLogEntity> findAllByOrderByChainIndexAsc();
}
