package io.audita.api.controller;

import io.audita.api.dto.request.InviteUserRequest;
import io.audita.api.dto.response.PageResponse;
import io.audita.api.dto.request.UpdateUserRequest;
import io.audita.api.dto.response.UserResponse;
import io.audita.infrastructure.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'users.view')")
    public PageResponse<UserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(userService.listUsers(pageable), UserResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.view') or #id.toString() == authentication.name")
    public UserResponse getUser(@PathVariable UUID id) {
        return UserResponse.from(userService.getUser(id));
    }

    @PostMapping("/invite")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.manage')")
    public ResponseEntity<UserResponse> inviteUser(@Valid @RequestBody InviteUserRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        UUID invitedById = UUID.fromString(principal.getUsername());
        var user = userService.inviteUser(req.email(), req.fullName(), req.roleId(), req.roleIds(), req.groupIds(), invitedById);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.manage')")
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return UserResponse.from(userService.updateUser(id, req.fullName(), req.roleId(), req.roleIds()));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        UUID requesterId = UUID.fromString(principal.getUsername());
        userService.deactivateUser(id, requesterId);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateUser(@PathVariable UUID id) {
        userService.reactivateUser(id);
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.manage')")
    public UserResponse resendInvite(@PathVariable UUID id) {
        return UserResponse.from(userService.resendInvite(id));
    }

    @DeleteMapping("/{id}/invite")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvite(@PathVariable UUID id) {
        userService.cancelInvite(id);
    }

    @GetMapping("/search")
    @PreAuthorize("@authz.hasPermission(authentication, 'users.view')")
    public List<UserResponse> searchUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "20") int limit) {
        return userService.searchUsers(query, limit).stream()
                .map(UserResponse::from)
                .toList();
    }
}
