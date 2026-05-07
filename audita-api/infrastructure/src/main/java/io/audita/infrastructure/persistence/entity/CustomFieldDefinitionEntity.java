package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "custom_field_definitions")
public class CustomFieldDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String label;

    @Column(name = "field_type", nullable = false)
    private String fieldType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected CustomFieldDefinitionEntity() {}

    public CustomFieldDefinitionEntity(String label, String fieldType,
                                       List<String> options, boolean isRequired,
                                       int displayOrder) {
        this.label = label;
        this.fieldType = fieldType;
        this.options = options;
        this.isRequired = isRequired;
        this.displayOrder = displayOrder;
    }

    public UUID getId() { return id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { isRequired = required; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
