package com.fincomp.clients.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fincomp.tenancy.entity.Organization;

@Entity
@Table(name = "clients")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "id_document_type", nullable = false)
    private String idDocumentType; // BI, NUIT, PASSPORT

    @Column(name = "id_document_number", nullable = false)
    private String idDocumentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ClientType clientType = ClientType.INDIVIDUAL;

    @Column(name = "nuib")
    private String nuib;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating", nullable = false)
    private RiskRating riskRating = RiskRating.LOW;

    @Column(name = "risk_rating_updated_at")
    private LocalDate riskRatingUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "due_diligence_regime", nullable = false)
    private DueDiligenceRegime dueDiligenceRegime = DueDiligenceRegime.NORMAL;

    @Column(name = "is_pep", nullable = false)
    private boolean pep = false;

    @Column(name = "pep_category")
    private String pepCategory;

    @Column(name = "pep_last_reviewed_at")
    private LocalDate pepLastReviewedAt;

    @Column(name = "senior_management_approved", nullable = false)
    private boolean seniorManagementApproved = false;

    @Column(name = "kyc_complete", nullable = false)
    private boolean kycComplete = false;

    @Column(name = "sanctions_frozen", nullable = false)
    private boolean sanctionsFrozen = false;

    @Column(name = "sanctions_frozen_at")
    private Instant sanctionsFrozenAt;

    @Column(name = "sanctions_freeze_reason")
    private String sanctionsFreezeReason;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BeneficialOwner> beneficialOwners = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Organization organization;

    public Client() {
    }

    public Client(String fullName, String idDocumentType, String idDocumentNumber) {
        this.fullName = fullName;
        this.idDocumentType = idDocumentType;
        this.idDocumentNumber = idDocumentNumber;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdDocumentType() {
        return idDocumentType;
    }

    public void setIdDocumentType(String idDocumentType) {
        this.idDocumentType = idDocumentType;
    }

    public String getIdDocumentNumber() {
        return idDocumentNumber;
    }

    public void setIdDocumentNumber(String idDocumentNumber) {
        this.idDocumentNumber = idDocumentNumber;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getNuib() {
        return nuib;
    }

    public void setNuib(String nuib) {
        this.nuib = nuib;
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(RiskRating riskRating) {
        this.riskRating = riskRating;
    }

    public LocalDate getRiskRatingUpdatedAt() {
        return riskRatingUpdatedAt;
    }

    public void setRiskRatingUpdatedAt(LocalDate riskRatingUpdatedAt) {
        this.riskRatingUpdatedAt = riskRatingUpdatedAt;
    }

    public DueDiligenceRegime getDueDiligenceRegime() {
        return dueDiligenceRegime;
    }

    public void setDueDiligenceRegime(DueDiligenceRegime dueDiligenceRegime) {
        this.dueDiligenceRegime = dueDiligenceRegime;
    }

    public boolean isPep() {
        return pep;
    }

    public void setPep(boolean pep) {
        this.pep = pep;
    }

    public String getPepCategory() {
        return pepCategory;
    }

    public void setPepCategory(String pepCategory) {
        this.pepCategory = pepCategory;
    }

    public LocalDate getPepLastReviewedAt() {
        return pepLastReviewedAt;
    }

    public void setPepLastReviewedAt(LocalDate pepLastReviewedAt) {
        this.pepLastReviewedAt = pepLastReviewedAt;
    }

    public boolean isSeniorManagementApproved() {
        return seniorManagementApproved;
    }

    public void setSeniorManagementApproved(boolean seniorManagementApproved) {
        this.seniorManagementApproved = seniorManagementApproved;
    }

    public boolean isKycComplete() {
        return kycComplete;
    }

    public void setKycComplete(boolean kycComplete) {
        this.kycComplete = kycComplete;
    }

    public boolean isSanctionsFrozen() {
        return sanctionsFrozen;
    }

    public void setSanctionsFrozen(boolean sanctionsFrozen) {
        this.sanctionsFrozen = sanctionsFrozen;
    }

    public Instant getSanctionsFrozenAt() {
        return sanctionsFrozenAt;
    }

    public void setSanctionsFrozenAt(Instant sanctionsFrozenAt) {
        this.sanctionsFrozenAt = sanctionsFrozenAt;
    }

    public String getSanctionsFreezeReason() {
        return sanctionsFreezeReason;
    }

    public void setSanctionsFreezeReason(String sanctionsFreezeReason) {
        this.sanctionsFreezeReason = sanctionsFreezeReason;
    }

    public List<BeneficialOwner> getBeneficialOwners() {
        return beneficialOwners;
    }

    public void setBeneficialOwners(List<BeneficialOwner> beneficialOwners) {
        this.beneficialOwners = beneficialOwners;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
}
