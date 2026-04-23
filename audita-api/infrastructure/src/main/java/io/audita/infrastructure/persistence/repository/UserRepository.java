package io.audita.infrastructure.persistence.repository;

import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<UserEntity> findByStatus(UserStatus status, Pageable pageable);

    List<UserEntity> findByFullNameContainingIgnoreCaseAndStatus(String name, UserStatus status, Pageable pageable);
}
