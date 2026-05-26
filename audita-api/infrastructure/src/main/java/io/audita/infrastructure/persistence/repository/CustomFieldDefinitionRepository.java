package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.CustomFieldDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinitionEntity, UUID> {

    long deleteByIsSampleTrue();
    long countByIsSampleTrue();

    List<CustomFieldDefinitionEntity> findAllByOrderByDisplayOrderAsc();
}
