package com.complyz.reporting.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.complyz.clients.entity.Client;
import com.complyz.monitoring.entity.ThresholdRule;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "currency_transaction_reports")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CurrencyTransactionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "beneficialOwners"})
    private Client client;

    @Column(name = "aggregation_period_start", nullable = false)
    private LocalDate aggregationPeriodStart;

    @Column(name = "aggregation_period_end", nullable = false)
    private LocalDate aggregationPeriodEnd;

    @Column(name = "aggregate_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal aggregateValue;

    @Column(name = "currency", nullable = false)
    private String currency = "MZN";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "threshold_rule_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ThresholdRule thresholdRule;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    public CurrencyTransactionReport() {
    }

    public CurrencyTransactionReport(Client client, LocalDate start, LocalDate end, BigDecimal aggregateValue, ThresholdRule rule) {
        this.client = client;
        this.aggregationPeriodStart = start;
        this.aggregationPeriodEnd = end;
        this.aggregateValue = aggregateValue;
        this.thresholdRule = rule;
        this.currency = "MZN";
    }

    public void markAsSubmitted() {
        if (submittedAt != null) {
            throw new IllegalStateException("Relatório CTR já foi submetido e não pode ser alterado");
        }
        this.submittedAt = Instant.now();
        this.retentionUntil = LocalDate.ofInstant(this.submittedAt, ZoneId.systemDefault()).plusYears(10);
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LocalDate getAggregationPeriodStart() {
        return aggregationPeriodStart;
    }

    public void setAggregationPeriodStart(LocalDate aggregationPeriodStart) {
        this.aggregationPeriodStart = aggregationPeriodStart;
    }

    public LocalDate getAggregationPeriodEnd() {
        return aggregationPeriodEnd;
    }

    public void setAggregationPeriodEnd(LocalDate aggregationPeriodEnd) {
        this.aggregationPeriodEnd = aggregationPeriodEnd;
    }

    public BigDecimal getAggregateValue() {
        return aggregateValue;
    }

    public void setAggregateValue(BigDecimal aggregateValue) {
        this.aggregateValue = aggregateValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ThresholdRule getThresholdRule() {
        return thresholdRule;
    }

    public void setThresholdRule(ThresholdRule thresholdRule) {
        this.thresholdRule = thresholdRule;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDate getRetentionUntil() {
        return retentionUntil;
    }

    public void setRetentionUntil(LocalDate retentionUntil) {
        this.retentionUntil = retentionUntil;
    }
}
