package io.audita.infrastructure.persistence.repository;

import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    long deleteByIsSampleTrue();
    long countByIsSampleTrue();

    @Override
    @EntityGraph(attributePaths = { "role", "roles", "roles.permissions" })
    Optional<UserEntity> findById(UUID id);

    @Override
    Page<UserEntity> findAll(Pageable pageable);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    Page<UserEntity> findByStatus(UserStatus status, Pageable pageable);

    List<UserEntity> findByFullNameContainingIgnoreCaseAndStatus(String name, UserStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "role", "roles" })
    List<UserEntity> findDistinctByRoles_NameInAndStatusOrderByFullNameAsc(List<String> roleNames, UserStatus status);

    @EntityGraph(attributePaths = { "role", "roles" })
    List<UserEntity> findByIdInAndStatusOrderByFullNameAsc(List<UUID> userIds, UserStatus status);

    @EntityGraph(attributePaths = { "role", "roles" })
    List<UserEntity> findByStatusAndFullNameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCaseOrderByFullNameAsc(
            UserStatus fullNameStatus,
            String fullName,
            UserStatus emailStatus,
            String email);

    long countByRole_NameAndStatus(String roleName, UserStatus status);

    long countDistinctByRoles_NameAndStatus(String roleName, UserStatus status);
}
