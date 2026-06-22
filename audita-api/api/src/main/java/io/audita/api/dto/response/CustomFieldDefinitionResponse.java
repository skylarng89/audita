package io.audita.api.dto.response;

import io.audita.application.port.CustomFieldAdminPort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CustomFieldDefinitionResponse(
        UUID id,
        String label,
        String fieldType,
        List<String> options,
        boolean isRequired,
        int displayOrder,
        BigDecimal minValue,
        BigDecimal maxValue,
        OffsetDateTime createdAt
) {
    public static CustomFieldDefinitionResponse from(CustomFieldAdminPort.FieldDefinition def) {
        return new CustomFieldDefinitionResponse(
                def.id(),
                def.label(),
                def.fieldType(),
                def.options(),
                def.isRequired(),
                def.displayOrder(),
                def.minValue(),
                def.maxValue(),
                def.createdAt()
        );
    }
}
