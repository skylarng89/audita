package io.audita.api.controller;

import io.audita.api.dto.request.UpsertDepartmentRequest;
import io.audita.api.dto.response.DepartmentResponse;
import io.audita.infrastructure.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings/departments")
public class DepartmentAdminController {

    private final DepartmentService departmentService;

    public DepartmentAdminController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public List<DepartmentResponse> listAll() {
        return departmentService.listAll().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public List<DepartmentResponse> listActive() {
        return departmentService.listActive().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@Valid @RequestBody UpsertDepartmentRequest request) {
        return DepartmentResponse.from(
                departmentService.create(request.name(), request.code(), request.isActive(), request.displayOrder()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public DepartmentResponse update(@PathVariable UUID id,
                                     @Valid @RequestBody UpsertDepartmentRequest request) {
        return DepartmentResponse.from(
                departmentService.update(id, request.name(), request.code(), request.isActive(), request.displayOrder()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        departmentService.deactivate(id);
    }
}
