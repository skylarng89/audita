package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChangeRequestCustomFieldRepository extends JpaRepository<ChangeRequestCustomFieldEntity, ChangeRequestCustomFieldId> {

    List<ChangeRequestCustomFieldEntity> findByIdChangeRequestId(UUID changeRequestId);

    void deleteByIdChangeRequestId(UUID changeRequestId);
}
