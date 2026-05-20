package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.PermissionEntity;
import io.audita.infrastructure.persistence.entity.RoleEntity;
import io.audita.infrastructure.persistence.repository.PermissionRepository;
import io.audita.infrastructure.persistence.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public List<RoleEntity> listRoles() {
        return roleRepository.findAll();
    }

    public RoleEntity getRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Role not found."));
    }

    public RoleEntity createCustomRole(String name, String description, List<String> permissionCodes) {
        if (name == null || name.isBlank()) {
            throw new DomainNotPermittedException("INVALID_ROLE", "Role name is required.");
        }
        if (roleRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new DomainNotPermittedException("DUPLICATE_ROLE", "A role with this name already exists.");
        }

        Set<PermissionEntity> permissions = resolvePermissions(permissionCodes);
        denyPermissionOverlap(permissions, null);

        RoleEntity role = new RoleEntity(name.trim(), description);
        role.setSystem(false);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    public RoleEntity updateRolePermissions(UUID roleId, List<String> permissionCodes) {
        RoleEntity role = getRole(roleId);
        if (role.isSystem()) {
            throw new DomainNotPermittedException("SYSTEM_ROLE_IMMUTABLE",
                    "System roles cannot be modified.");
        }

        Set<PermissionEntity> permissions = resolvePermissions(permissionCodes);
        denyPermissionOverlap(permissions, roleId);

        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    private Set<PermissionEntity> resolvePermissions(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new DomainNotPermittedException("INVALID_ROLE", "At least one permission is required.");
        }

        LinkedHashSet<String> normalizedCodes = permissionCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (normalizedCodes.size() != permissionCodes.stream().filter(code -> code != null && !code.isBlank())
                .count()) {
            throw new DomainNotPermittedException("OVERLAPPING_PERMISSIONS",
                    "Permission codes must be unique within a role.");
        }

        List<PermissionEntity> permissions = permissionRepository.findByCodeIn(normalizedCodes);
        if (permissions.size() != normalizedCodes.size()) {
            throw new DomainNotPermittedException("INVALID_PERMISSION", "One or more permissions are invalid.");
        }
        return new LinkedHashSet<>(permissions);
    }

    private void denyPermissionOverlap(Collection<PermissionEntity> requestedPermissions, UUID selfRoleId) {
        Set<String> requestedCodes = requestedPermissions.stream()
                .map(PermissionEntity::getCode)
                .map(code -> code == null ? "" : code.toLowerCase())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (RoleEntity existingRole : roleRepository.findAll()) {
            if (selfRoleId != null && selfRoleId.equals(existingRole.getId())) {
                continue;
            }
            Set<String> existingCodes = existingRole.getPermissions().stream()
                    .map(PermissionEntity::getCode)
                    .map(code -> code == null ? "" : code.toLowerCase())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (existingCodes.equals(requestedCodes)) {
                throw new DomainNotPermittedException("OVERLAPPING_PERMISSIONS",
                        "A role with the same permission rules already exists.");
            }
        }
    }
}
