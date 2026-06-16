package io.audita.api.controller;

import io.audita.api.dto.request.BatchMembersRequest;
import io.audita.api.dto.request.CreateGroupRequest;
import io.audita.api.dto.request.GroupMemberRequest;
import io.audita.api.dto.response.GroupResponse;
import io.audita.api.dto.response.PageResponse;
import io.audita.api.dto.response.UserResponse;
import io.audita.api.security.CurrentUser;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<GroupResponse> listGroups(@PageableDefault(size = 20) Pageable pageable,
                                                   @RequestParam(required = false) Boolean active) {
        if (active != null && active) {
            return PageResponse.from(groupService.listActiveGroups(pageable), GroupResponse::from);
        }
        return PageResponse.from(groupService.listGroups(pageable), GroupResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public GroupResponse getGroup(@PathVariable UUID id) {
        return GroupResponse.from(groupService.getGroup(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest req,
                                                      @AuthenticationPrincipal UserDetails principal) {
        UUID createdById = UUID.fromString(principal.getUsername());
        var group = groupService.createGroup(req.name(), req.description(), createdById, req.isActive(), req.displayOrder(), req.memberIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(GroupResponse.from(group));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public GroupResponse updateGroup(@PathVariable UUID id,
                                     @Valid @RequestBody CreateGroupRequest req) {
        return GroupResponse.from(groupService.updateGroup(id, req.name(), req.description()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable UUID id, @CurrentUser UserPrincipal user) {
        groupService.deleteGroup(id, user.userId());
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("isAuthenticated()")
        public PageResponse<UserResponse> listMembers(@PathVariable UUID id,
                              @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(
            groupService.listMembers(id, pageable),
            m -> UserResponse.from(m.getUser())
        );
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable UUID id, @Valid @RequestBody GroupMemberRequest req) {
        groupService.addMember(id, req.userId());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        groupService.removeMember(id, userId);
    }

    @PostMapping("/{id}/members/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMembers(@PathVariable UUID id, @Valid @RequestBody BatchMembersRequest req,
                           @CurrentUser UserPrincipal user) {
        groupService.addMembers(id, req.userIds(), user.userId());
    }

    @DeleteMapping("/{id}/members/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMembers(@PathVariable UUID id, @Valid @RequestBody BatchMembersRequest req,
                              @CurrentUser UserPrincipal user) {
        groupService.removeMembers(id, req.userIds(), user.userId());
    }
}
