package io.audita.application.port;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CustomFieldAdminPort {

    List<FieldDefinition> listDefinitions();

    FieldDefinition createDefinition(String label,
                                     String fieldType,
                                     List<String> options,
                                     boolean isRequired,
                                     int displayOrder,
                                     BigDecimal minValue,
                                     BigDecimal maxValue);

    FieldDefinition updateDefinition(UUID id,
                                     String label,
                                     String fieldType,
                                     List<String> options,
                                     boolean isRequired,
                                     int displayOrder,
                                     BigDecimal minValue,
                                     BigDecimal maxValue);

    void deleteDefinition(UUID id);

    record FieldDefinition(
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
    }
}
