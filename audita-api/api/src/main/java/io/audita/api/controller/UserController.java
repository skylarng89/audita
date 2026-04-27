package io.audita.api.controller;

import io.audita.api.dto.request.InviteUserRequest;
import io.audita.api.dto.request.UpdateUserRequest;
import io.audita.api.dto.response.UserResponse;
import io.audita.infrastructure.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Page<UserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return userService.listUsers(pageable).map(UserResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #id.toString() == authentication.name")
    public UserResponse getUser(@PathVariable UUID id) {
        return UserResponse.from(userService.getUser(id));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> inviteUser(@Valid @RequestBody InviteUserRequest req,
                                                   @AuthenticationPrincipal UserDetails principal) {
        UUID invitedById = UUID.fromString(principal.getUsername());
        var user = userService.inviteUser(req.email(), req.fullName(), req.roleId(), invitedById);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return UserResponse.from(userService.updateUser(id, req.fullName(), req.roleId()));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateUser(@PathVariable UUID id) {
        userService.reactivateUser(id);
    }
}
