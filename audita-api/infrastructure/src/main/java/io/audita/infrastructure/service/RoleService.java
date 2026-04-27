package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.RoleEntity;
import io.audita.infrastructure.persistence.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleEntity> listRoles() {
        return roleRepository.findAll();
    }

    public RoleEntity getRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Role not found."));
    }
}
