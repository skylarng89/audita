package io.audita.api.controller;

import io.audita.api.dto.request.CreateCustomRoleRequest;
import io.audita.api.dto.request.UpdateRolePermissionsRequest;
import io.audita.api.dto.response.PermissionCatalogueResponse;
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
@PreAuthorize("@authz.hasPermission(authentication, 'roles.view')")
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

    @GetMapping("/permissions")
    public PermissionCatalogueResponse listPermissions() {
        return PermissionCatalogueResponse.from(roleService.listAllPermissions());
    }

    @PostMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'roles.manage')")
    public ResponseEntity<RoleResponse> createCustomRole(@Valid @RequestBody CreateCustomRoleRequest req) {
        var role = roleService.createCustomRole(req.name(), req.description(), req.permissionCodes());
        return new ResponseEntity<>(RoleResponse.from(role), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/permissions")
    @PreAuthorize("@authz.hasPermission(authentication, 'roles.manage')")
    public RoleResponse updateRolePermissions(@PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest req) {
        return RoleResponse.from(roleService.updateRolePermissions(id, req.permissionCodes()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.hasPermission(authentication, 'roles.manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
    }
}
