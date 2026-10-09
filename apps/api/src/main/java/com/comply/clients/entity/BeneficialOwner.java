package com.comply.clients.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "beneficial_owners")
public class BeneficialOwner {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  @JsonIgnore
  private Client client;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private BeneficialOwnerRole role;

  @Column(name = "ownership_percentage", precision = 5, scale = 2)
  private BigDecimal ownershipPercentage;

  @Column(name = "verified_at")
  private LocalDate verifiedAt;

  public BeneficialOwner() {}

  public BeneficialOwner(
      Client client, String fullName, BeneficialOwnerRole role, BigDecimal ownershipPercentage) {
    this.client = client;
    this.fullName = fullName;
    this.role = role;
    this.ownershipPercentage = ownershipPercentage;
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

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public BeneficialOwnerRole getRole() {
    return role;
  }

  public void setRole(BeneficialOwnerRole role) {
    this.role = role;
  }

  public BigDecimal getOwnershipPercentage() {
    return ownershipPercentage;
  }

  public void setOwnershipPercentage(BigDecimal ownershipPercentage) {
    this.ownershipPercentage = ownershipPercentage;
  }

  public LocalDate getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDate verifiedAt) {
    this.verifiedAt = verifiedAt;
  }
}
