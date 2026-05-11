package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.OrgSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgSettingRepository extends JpaRepository<OrgSettingEntity, String> {
}