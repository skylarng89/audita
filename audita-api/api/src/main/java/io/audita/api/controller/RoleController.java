package io.audita.api.controller;

import io.audita.api.dto.response.RoleResponse;
import io.audita.infrastructure.service.RoleService;
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
}
