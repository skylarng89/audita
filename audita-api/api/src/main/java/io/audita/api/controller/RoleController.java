package io.audita.api.controller;

import io.audita.api.dto.request.CreateCustomRoleRequest;
import io.audita.api.dto.request.UpdateRolePermissionsRequest;
import io.audita.api.dto.response.RoleResponse;
import io.audita.infrastructure.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("isAuthenticated()")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<RoleResponse> listRoles() {
        return roleService.listRoles().stream().map(RoleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RoleResponse getRole(@PathVariable UUID id) {
        return RoleResponse.from(roleService.getRole(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RoleResponse> createCustomRole(@Valid @RequestBody CreateCustomRoleRequest req) {
        var role = roleService.createCustomRole(req.name(), req.description(), req.permissionCodes());
        return new ResponseEntity<>(RoleResponse.from(role), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public RoleResponse updateRolePermissions(@PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest req) {
        return RoleResponse.from(roleService.updateRolePermissions(id, req.permissionCodes()));
    }
}
