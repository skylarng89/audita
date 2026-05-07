package io.audita.api.controller;

import io.audita.api.dto.request.CustomFieldDefinitionRequest;
import io.audita.api.dto.response.CustomFieldDefinitionResponse;
import io.audita.application.port.CustomFieldAdminPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/custom-fields")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class CustomFieldAdminController {

    private final CustomFieldAdminPort customFieldAdminPort;

    public CustomFieldAdminController(CustomFieldAdminPort customFieldAdminPort) {
        this.customFieldAdminPort = customFieldAdminPort;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CustomFieldDefinitionResponse> list() {
        return customFieldAdminPort.listDefinitions().stream()
                .map(CustomFieldDefinitionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CustomFieldDefinitionResponse> create(
            @Valid @RequestBody CustomFieldDefinitionRequest req) {
        CustomFieldAdminPort.FieldDefinition created = customFieldAdminPort.createDefinition(
                req.label(),
                req.fieldType(),
                req.options(),
                req.isRequired(),
                req.displayOrder()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomFieldDefinitionResponse.from(created));
    }

    @PutMapping("/{id}")
    public CustomFieldDefinitionResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomFieldDefinitionRequest req) {
        return CustomFieldDefinitionResponse.from(
                customFieldAdminPort.updateDefinition(
                        id,
                        req.label(),
                        req.fieldType(),
                        req.options(),
                        req.isRequired(),
                        req.displayOrder()
                )
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        customFieldAdminPort.deleteDefinition(id);
    }
}
