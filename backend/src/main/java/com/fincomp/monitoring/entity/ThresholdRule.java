package com.fincomp.monitoring.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "threshold_rules")
public class ThresholdRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ThresholdRuleType type;

    @Column(name = "value_threshold", precision = 18, scale = 2)
    private BigDecimal valueThreshold;

    @Column(name = "aggregation_window_days")
    private Integer aggregationWindowDays = 30;

    @Column(name = "legal_basis", nullable = false)
    private String legalBasis;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public ThresholdRule() {
    }

    public ThresholdRule(String name, ThresholdRuleType type, BigDecimal valueThreshold, Integer aggregationWindowDays, String legalBasis) {
        this.name = name;
        this.type = type;
        this.valueThreshold = valueThreshold;
        this.aggregationWindowDays = aggregationWindowDays;
        this.legalBasis = legalBasis;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ThresholdRuleType getType() {
        return type;
    }

    public void setType(ThresholdRuleType type) {
        this.type = type;
    }

    public BigDecimal getValueThreshold() {
        return valueThreshold;
    }

    public void setValueThreshold(BigDecimal valueThreshold) {
        this.valueThreshold = valueThreshold;
    }

    public Integer getAggregationWindowDays() {
        return aggregationWindowDays;
    }

    public void setAggregationWindowDays(Integer aggregationWindowDays) {
        this.aggregationWindowDays = aggregationWindowDays;
    }

    @JsonIgnore
    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
