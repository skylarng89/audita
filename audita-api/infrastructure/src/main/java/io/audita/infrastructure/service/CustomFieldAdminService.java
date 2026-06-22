package io.audita.infrastructure.service;

import io.audita.application.port.CustomFieldAdminPort;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.CustomFieldDefinitionEntity;
import io.audita.infrastructure.persistence.repository.CustomFieldDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CustomFieldAdminService implements CustomFieldAdminPort {

    private static final Set<String> VALID_FIELD_TYPES =
            Set.of("TEXT", "NUMBER", "DATE", "DROPDOWN", "CHECKBOX");

    private final CustomFieldDefinitionRepository repository;

    public CustomFieldAdminService(CustomFieldDefinitionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDefinition> listDefinitions() {
        return repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    public FieldDefinition createDefinition(String label,
                                             String fieldType,
                                             List<String> options,
                                             boolean isRequired,
                                             int displayOrder,
                                             BigDecimal minValue,
                                             BigDecimal maxValue) {
        validateFieldType(fieldType);
        validateDropdownOptions(fieldType, options);
        validateMinMax(fieldType, minValue, maxValue);
        CustomFieldDefinitionEntity entity =
                new CustomFieldDefinitionEntity(label, fieldType.toUpperCase(),
                        options, isRequired, displayOrder, minValue, maxValue);
        return toRecord(repository.save(entity));
    }

    @Override
    public FieldDefinition updateDefinition(UUID id,
                                             String label,
                                             String fieldType,
                                             List<String> options,
                                             boolean isRequired,
                                             int displayOrder,
                                             BigDecimal minValue,
                                             BigDecimal maxValue) {
        validateFieldType(fieldType);
        validateDropdownOptions(fieldType, options);
        validateMinMax(fieldType, minValue, maxValue);
        CustomFieldDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND",
                        "Custom field definition not found."));
        entity.setLabel(label);
        entity.setFieldType(fieldType.toUpperCase());
        entity.setOptions("DROPDOWN".equalsIgnoreCase(fieldType) ? options : null);
        entity.setRequired(isRequired);
        entity.setDisplayOrder(displayOrder);
        entity.setMinValue(minValue);
        entity.setMaxValue(maxValue);
        return toRecord(repository.save(entity));
    }

    @Override
    public void deleteDefinition(UUID id) {
        if (!repository.existsById(id)) {
            throw new DomainNotPermittedException("NOT_FOUND",
                    "Custom field definition not found.");
        }
        repository.deleteById(id);
    }

    private void validateFieldType(String fieldType) {
        if (fieldType == null || !VALID_FIELD_TYPES.contains(fieldType.toUpperCase())) {
            throw new DomainNotPermittedException("INVALID_FIELD_TYPE",
                    "fieldType must be one of: TEXT, NUMBER, DATE, DROPDOWN, CHECKBOX.");
        }
    }

    private void validateDropdownOptions(String fieldType, List<String> options) {
        if ("DROPDOWN".equalsIgnoreCase(fieldType) && (options == null || options.isEmpty())) {
            throw new DomainNotPermittedException("MISSING_OPTIONS",
                    "DROPDOWN fields require at least one option.");
        }
    }

    private void validateMinMax(String fieldType, BigDecimal minValue, BigDecimal maxValue) {
        boolean hasMinMax = minValue != null || maxValue != null;
        if (hasMinMax && !"NUMBER".equalsIgnoreCase(fieldType)) {
            throw new DomainNotPermittedException("INVALID_MIN_MAX",
                    "minValue and maxValue are only valid for NUMBER fields.");
        }
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
            throw new DomainNotPermittedException("INVALID_MIN_MAX",
                    "minValue must be less than or equal to maxValue.");
        }
    }

    private FieldDefinition toRecord(CustomFieldDefinitionEntity e) {
        return new FieldDefinition(
                e.getId(),
                e.getLabel(),
                e.getFieldType(),
                e.getOptions(),
                e.isRequired(),
                e.getDisplayOrder(),
                e.getMinValue(),
                e.getMaxValue(),
                e.getCreatedAt()
        );
    }
}
