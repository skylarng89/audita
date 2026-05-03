package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "group_members")
public class GroupMemberEntity {

    @EmbeddedId
    private GroupMemberId id = new GroupMemberId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();

    protected GroupMemberEntity() {}

    public GroupMemberEntity(GroupEntity group, UserEntity user) {
        this.group = group;
        this.user = user;
        this.id.setGroupId(group.getId());
        this.id.setUserId(user.getId());
    }

    public GroupMemberId getId() { return id; }
    public GroupEntity getGroup() { return group; }
    public UserEntity getUser() { return user; }
    public OffsetDateTime getAddedAt() { return addedAt; }
}
