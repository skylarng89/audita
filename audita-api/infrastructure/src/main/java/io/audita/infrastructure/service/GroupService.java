package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.tenant.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository,
                        AuditLogService auditLogService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<GroupEntity> listGroups(Pageable pageable) {
        Page<GroupEntity> page = groupRepository.findAll(pageable);
        page.getContent().forEach(g -> g.setMemberCount((int) groupMemberRepository.countByGroupId(g.getId())));
        return page;
    }

    @Transactional(readOnly = true)
    public Page<GroupEntity> listActiveGroups(Pageable pageable) {
        Page<GroupEntity> page = groupRepository.findAllByIsActiveTrue(pageable);
        page.getContent().forEach(g -> g.setMemberCount((int) groupMemberRepository.countByGroupId(g.getId())));
        return page;
    }

    @Transactional(readOnly = true)
    public GroupEntity getGroup(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Group not found."));
    }

    public GroupEntity createGroup(String name, String description, UUID createdByUserId) {
        return createGroup(name, description, createdByUserId, true, 0, List.of());
    }

    public GroupEntity createGroup(String name, String description, UUID createdByUserId, boolean isActive, int displayOrder, List<UUID> memberIds) {
        if (groupRepository.existsByName(name)) {
            throw new DomainNotPermittedException("NAME_TAKEN",
                    "A group named '" + name + "' already exists.");
        }

        UserEntity createdBy = userRepository.findById(createdByUserId).orElse(null);
        GroupEntity group = new GroupEntity(name, description, createdBy);
        group.setActive(isActive);
        group.setDisplayOrder(displayOrder);
        group = groupRepository.save(group);

        if (memberIds != null && !memberIds.isEmpty()) {
            addMembers(group.getId(), memberIds, createdByUserId);
        }

        return group;
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

    public void deleteGroup(UUID id, UUID actorUserId) {
        GroupEntity group = getGroup(id);

        List<GroupMemberEntity> members = groupMemberRepository.findAllByGroupId(id);
        if (!members.isEmpty()) {
            groupMemberRepository.deleteAll(members);
            log.info("Unassigned {} members from group id={} name={}", members.size(), id, group.getName());
        }

        groupRepository.delete(group);
        log.info("Deleted group id={} name={}", id, group.getName());

        Map<String, Object> payload = new HashMap<>();
        payload.put("groupName", group.getName());
        auditLogService.log("GROUP_DELETED", "group", id, actorUserId,
                resolveActorEmail(actorUserId), payload, RequestContext.getCurrentIp());
    }

    // ── Members ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GroupMemberEntity> listMembers(UUID groupId, Pageable pageable) {
        getGroup(groupId);
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

    public List<GroupMemberEntity> addMembers(UUID groupId, List<UUID> userIds, UUID actorUserId) {
        GroupEntity group = getGroup(groupId);
        List<GroupMemberEntity> added = new ArrayList<>();

        for (UUID userId : userIds) {
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
                UserEntity user = userRepository.findById(userId)
                        .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "User not found: " + userId));
                GroupMemberEntity member = new GroupMemberEntity(group, user);
                added.add(groupMemberRepository.save(member));
            }
        }

        if (!added.isEmpty()) {
            log.info("Added {} members to group id={}", added.size(), groupId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userIds", userIds.stream().map(UUID::toString).toList());
            payload.put("count", added.size());
            auditLogService.log("GROUP_MEMBERS_ADDED", "group", groupId, actorUserId,
                    resolveActorEmail(actorUserId), payload, RequestContext.getCurrentIp());
        }

        return added;
    }

    public void removeMembers(UUID groupId, List<UUID> userIds, UUID actorUserId) {
        for (UUID userId : userIds) {
            groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
        }
        log.info("Removed {} members from group id={}", userIds.size(), groupId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userIds", userIds.stream().map(UUID::toString).toList());
        payload.put("count", userIds.size());
        auditLogService.log("GROUP_MEMBERS_REMOVED", "group", groupId, actorUserId,
                resolveActorEmail(actorUserId), payload, RequestContext.getCurrentIp());
    }

    private
        String resolveActorEmail(UUID actorUserId) {
            if (actorUserId == null) {
                return null;
            }
            return userRepository.findById(actorUserId)
                    .map(UserEntity::getEmail)
                    .orElse(null);
        }
}
