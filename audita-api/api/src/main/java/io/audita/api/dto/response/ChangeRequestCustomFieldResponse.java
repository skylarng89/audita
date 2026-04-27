package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldEntity;

import java.util.UUID;

public record ChangeRequestCustomFieldResponse(
        UUID fieldId,
        String value
) {
    public static ChangeRequestCustomFieldResponse from(ChangeRequestCustomFieldEntity entity) {
        return new ChangeRequestCustomFieldResponse(entity.getId().getFieldId(), entity.getValue());
    }
}
