package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ChangeRequestCustomFieldId implements Serializable {

    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    protected ChangeRequestCustomFieldId() {}

    public ChangeRequestCustomFieldId(UUID changeRequestId, UUID fieldId) {
        this.changeRequestId = changeRequestId;
        this.fieldId = fieldId;
    }

    public UUID getChangeRequestId() {
        return changeRequestId;
    }

    public UUID getFieldId() {
        return fieldId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChangeRequestCustomFieldId that)) {
            return false;
        }
        return Objects.equals(changeRequestId, that.changeRequestId)
                && Objects.equals(fieldId, that.fieldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeRequestId, fieldId);
    }
}
