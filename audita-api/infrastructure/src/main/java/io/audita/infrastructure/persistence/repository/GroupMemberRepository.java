package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.GroupMemberEntity;
import io.audita.infrastructure.persistence.entity.GroupMemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {

    @Query("SELECT m FROM GroupMemberEntity m WHERE m.group.id = :groupId")
    Page<GroupMemberEntity> findByGroupId(@Param("groupId") UUID groupId, Pageable pageable);

    @Query("SELECT m FROM GroupMemberEntity m WHERE m.user.id = :userId")
    List<GroupMemberEntity> findByUserId(@Param("userId") UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    void deleteByGroupIdAndUserId(UUID groupId, UUID userId);
}
