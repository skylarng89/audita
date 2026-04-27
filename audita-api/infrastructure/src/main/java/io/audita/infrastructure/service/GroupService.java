package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<GroupEntity> listGroups(Pageable pageable) {
        return groupRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public GroupEntity getGroup(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Group not found."));
    }

    public GroupEntity createGroup(String name, String description, UUID createdByUserId) {
        if (groupRepository.existsByName(name)) {
            throw new DomainNotPermittedException("NAME_TAKEN",
                    "A group named '" + name + "' already exists.");
        }

        UserEntity createdBy = userRepository.findById(createdByUserId).orElse(null);
        GroupEntity group = new GroupEntity(name, description, createdBy);
        return groupRepository.save(group);
    }

    public GroupEntity updateGroup(UUID id, String name, String description) {
        GroupEntity group = getGroup(id);

        if (name != null && !name.isBlank() && !name.equals(group.getName())) {
            if (groupRepository.existsByName(name)) {
                throw new DomainNotPermittedException("NAME_TAKEN",
                        "A group named '" + name + "' already exists.");
            }
            group.setName(name);
        }

        if (description != null) {
            group.setDescription(description);
        }

        return groupRepository.save(group);
    }

    public void deleteGroup(UUID id) {
        GroupEntity group = getGroup(id);
        groupRepository.delete(group);
        log.info("Group deleted: id={} name={}", id, group.getName());
    }

    // ── Members ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GroupMemberEntity> listMembers(UUID groupId, Pageable pageable) {
        getGroup(groupId); // ensure group exists
        return groupMemberRepository.findByGroupId(groupId, pageable);
    }

    public void addMember(UUID groupId, UUID userId) {
        GroupEntity group = getGroup(groupId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "User not found."));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new DomainNotPermittedException("ALREADY_MEMBER",
                    "User is already a member of this group.");
        }

        groupMemberRepository.save(new GroupMemberEntity(group, user));
        log.info("Member added: groupId={} userId={}", groupId, userId);
    }

    public void removeMember(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new DomainNotPermittedException("NOT_FOUND",
                    "User is not a member of this group.");
        }
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("Member removed: groupId={} userId={}", groupId, userId);
    }
}
