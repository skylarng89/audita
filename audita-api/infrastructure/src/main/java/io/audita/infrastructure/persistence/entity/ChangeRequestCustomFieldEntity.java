package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "change_request_custom_fields")
public class ChangeRequestCustomFieldEntity {

    @EmbeddedId
    private ChangeRequestCustomFieldId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("changeRequestId")
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequestEntity changeRequest;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    protected ChangeRequestCustomFieldEntity() {}

    public ChangeRequestCustomFieldEntity(ChangeRequestEntity changeRequest, java.util.UUID fieldId, String value) {
        this.changeRequest = changeRequest;
        this.id = new ChangeRequestCustomFieldId(changeRequest.getId(), fieldId);
        this.value = value;
    }

    public ChangeRequestCustomFieldId getId() {
        return id;
    }

    public ChangeRequestEntity getChangeRequest() {
        return changeRequest;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
