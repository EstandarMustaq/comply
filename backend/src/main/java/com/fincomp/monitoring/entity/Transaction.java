package com.fincomp.monitoring.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fincomp.clients.entity.Client;
import com.fincomp.ingest.entity.ImportBatch;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "beneficialOwners"})
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ImportBatch importBatch;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency = "MZN";

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "counterparty_details", columnDefinition = "jsonb")
    private String counterpartyDetails;

    @Column(name = "retention_until", nullable = false)
    private LocalDate retentionUntil; // 10 anos (Art. 88 Aviso 10/GBM/2024)

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Transaction() {
    }

    public Transaction(Client client, BigDecimal amount, String currency, LocalDate transactionDate, TransactionType type) {
        this.client = client;
        this.amount = amount;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "MZN";
        this.transactionDate = transactionDate;
        this.type = type;
        this.retentionUntil = transactionDate.plusYears(10);
        this.createdAt = Instant.now();
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

    public ImportBatch getImportBatch() {
        return importBatch;
    }

    public void setImportBatch(ImportBatch importBatch) {
        this.importBatch = importBatch;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
        if (transactionDate != null && this.retentionUntil == null) {
            this.retentionUntil = transactionDate.plusYears(10);
        }
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCounterpartyDetails() {
        return counterpartyDetails;
    }

    public void setCounterpartyDetails(String counterpartyDetails) {
        this.counterpartyDetails = counterpartyDetails;
    }

    public LocalDate getRetentionUntil() {
        return retentionUntil;
    }

    public void setRetentionUntil(LocalDate retentionUntil) {
        this.retentionUntil = retentionUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
