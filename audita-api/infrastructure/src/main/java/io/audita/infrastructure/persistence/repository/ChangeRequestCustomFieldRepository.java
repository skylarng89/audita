package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ChangeRequestCustomFieldRepository extends JpaRepository<ChangeRequestCustomFieldEntity, ChangeRequestCustomFieldId> {

    List<ChangeRequestCustomFieldEntity> findByIdChangeRequestId(UUID changeRequestId);

    void deleteByIdChangeRequestId(UUID changeRequestId);

    @Modifying
    @Query("DELETE FROM ChangeRequestCustomFieldEntity cf WHERE cf.isSample = true")
    long deleteByIsSampleTrue();

    @Query("SELECT COUNT(cf) FROM ChangeRequestCustomFieldEntity cf WHERE cf.isSample = true")
    long countByIsSampleTrue();
}
