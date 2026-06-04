package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    List<DepartmentEntity> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    boolean existsByName(String name);

    List<DepartmentEntity> findAllByOrderByDisplayOrderAscNameAsc();
}
