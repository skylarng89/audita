package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
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

    @Column(name = "min_value")
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue;

    @Column(name = "is_sample", nullable = false)
    private boolean isSample = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected CustomFieldDefinitionEntity() {}

    public CustomFieldDefinitionEntity(String label, String fieldType,
                                       List<String> options, boolean isRequired,
                                       int displayOrder) {
        this(label, fieldType, options, isRequired, displayOrder, null, null);
    }

    public CustomFieldDefinitionEntity(String label, String fieldType,
                                       List<String> options, boolean isRequired,
                                       int displayOrder,
                                       BigDecimal minValue, BigDecimal maxValue) {
        this.label = label;
        this.fieldType = fieldType;
        this.options = options;
        this.isRequired = isRequired;
        this.displayOrder = displayOrder;
        this.minValue = minValue;
        this.maxValue = maxValue;
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
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isSample() { return isSample; }
    public void setSample(boolean sample) { isSample = sample; }
}
