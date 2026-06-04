package io.audita.infrastructure.service;

import io.audita.domain.exception.InvalidRequestException;
import io.audita.domain.exception.NotFoundException;
import io.audita.infrastructure.persistence.entity.DepartmentEntity;
import io.audita.infrastructure.persistence.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentEntity> listAll() {
        return repository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<DepartmentEntity> listActive() {
        return repository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public DepartmentEntity create(String name, String code, boolean isActive, int displayOrder) {
        if (repository.existsByName(name)) {
            throw new InvalidRequestException("DUPLICATE_NAME", "Department name already exists.");
        }
        DepartmentEntity entity = new DepartmentEntity();
        entity.setName(name);
        entity.setCode(code);
        entity.setActive(isActive);
        entity.setDisplayOrder(displayOrder);
        return repository.save(entity);
    }

    public DepartmentEntity update(UUID id, String name, String code, boolean isActive, int displayOrder) {
        DepartmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department", id));
        if (!entity.getName().equals(name) && repository.existsByName(name)) {
            throw new InvalidRequestException("DUPLICATE_NAME", "Department name already exists.");
        }
        entity.setName(name);
        entity.setCode(code);
        entity.setActive(isActive);
        entity.setDisplayOrder(displayOrder);
        return repository.save(entity);
    }

    public void deactivate(UUID id) {
        DepartmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department", id));
        entity.setActive(false);
        repository.save(entity);
    }
}
