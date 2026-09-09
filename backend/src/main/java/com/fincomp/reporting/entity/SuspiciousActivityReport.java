package com.fincomp.reporting.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fincomp.flags.entity.Flag;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "suspicious_activity_reports")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SuspiciousActivityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flag_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "transaction", "client", "reviewedBy"})
    private Flag flag;

    @Column(name = "reporting_institution", nullable = false)
    private String reportingInstitution;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_holder", nullable = false)
    private String accountHolder;

    @Column(name = "transaction_executor")
    private String transactionExecutor;

    @Column(name = "monetary_value", precision = 18, scale = 2)
    private BigDecimal monetaryValue;

    @Column(name = "suspicion_description", nullable = false, columnDefinition = "text")
    private String suspicionDescription;

    @Column(name = "business_relationship_description", nullable = false, columnDefinition = "text")
    private String businessRelationshipDescription;

    @Column(name = "internal_employee_involved", nullable = false)
    private boolean internalEmployeeInvolved = false;

    @Column(name = "voluntary_statement", columnDefinition = "text")
    private String voluntaryStatement;

    @Column(name = "ocos_name", nullable = false)
    private String ocosName;

    @Column(name = "gifim_registration_number")
    private String gifimRegistrationNumber;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "retention_until")
    private LocalDate retentionUntil; // 10 anos a contar da elaboração/submissão (Art. 89)

    public SuspiciousActivityReport() {
    }

    public void markAsSubmitted(String gifimRegNumber) {
        this.submittedAt = Instant.now();
        this.gifimRegistrationNumber = gifimRegNumber;
        this.retentionUntil = LocalDate.ofInstant(this.submittedAt, ZoneId.systemDefault()).plusYears(10);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Flag getFlag() {
        return flag;
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    public String getReportingInstitution() {
        return reportingInstitution;
    }

    public void setReportingInstitution(String reportingInstitution) {
        this.reportingInstitution = reportingInstitution;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public String getTransactionExecutor() {
        return transactionExecutor;
    }

    public void setTransactionExecutor(String transactionExecutor) {
        this.transactionExecutor = transactionExecutor;
    }

    public BigDecimal getMonetaryValue() {
        return monetaryValue;
    }

    public void setMonetaryValue(BigDecimal monetaryValue) {
        this.monetaryValue = monetaryValue;
    }

    public String getSuspicionDescription() {
        return suspicionDescription;
    }

    public void setSuspicionDescription(String suspicionDescription) {
        this.suspicionDescription = suspicionDescription;
    }

    public String getBusinessRelationshipDescription() {
        return businessRelationshipDescription;
    }

    public void setBusinessRelationshipDescription(String businessRelationshipDescription) {
        this.businessRelationshipDescription = businessRelationshipDescription;
    }

    public boolean isInternalEmployeeInvolved() {
        return internalEmployeeInvolved;
    }

    public void setInternalEmployeeInvolved(boolean internalEmployeeInvolved) {
        this.internalEmployeeInvolved = internalEmployeeInvolved;
    }

    public String getVoluntaryStatement() {
        return voluntaryStatement;
    }

    public void setVoluntaryStatement(String voluntaryStatement) {
        this.voluntaryStatement = voluntaryStatement;
    }

    public String getOcosName() {
        return ocosName;
    }

    public void setOcosName(String ocosName) {
        this.ocosName = ocosName;
    }

    public String getGifimRegistrationNumber() {
        return gifimRegistrationNumber;
    }

    public void setGifimRegistrationNumber(String gifimRegistrationNumber) {
        this.gifimRegistrationNumber = gifimRegistrationNumber;
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
