package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>,
        JpaSpecificationExecutor<AuditLogEntity> {

    @Query(value = "SELECT COALESCE(MAX(chain_index), 0) FROM audit_log", nativeQuery = true)
    long getMaxChainIndex();

    @Query(value = "SELECT record_hash FROM audit_log ORDER BY chain_index DESC LIMIT 1", nativeQuery = true)
    byte[] findLastRecordHash();

    List<AuditLogEntity> findAllByOrderByChainIndexAsc();
}
