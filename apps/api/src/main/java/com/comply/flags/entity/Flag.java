package com.comply.flags.entity;

import com.comply.clients.entity.Client;
import com.comply.monitoring.entity.Transaction;
import com.comply.users.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flags")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Flag {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "transaction_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "client", "importBatch"})
  private Transaction transaction;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "client_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "beneficialOwners"})
  private Client client;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private FlagType type;

  @Column(name = "reason", nullable = false, columnDefinition = "text")
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FlagStatus status = FlagStatus.PENDING;

  @Column(name = "sla_deadline")
  private Instant slaDeadline;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "reviewed_by")
  @JsonIgnoreProperties({
    "hibernateLazyInitializer",
    "handler",
    "passwordHash",
    "mfaSecretEncrypted",
    "mfaBackupCodesHash"
  })
  private User reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "review_note", columnDefinition = "text")
  private String reviewNote;

  public Flag() {}

  public Flag(
      Client client, Transaction transaction, FlagType type, String reason, Instant slaDeadline) {
    this.client = client;
    this.transaction = transaction;
    this.type = type;
    this.reason = reason;
    this.slaDeadline = slaDeadline;
    this.status = FlagStatus.PENDING;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public void setTransaction(Transaction transaction) {
    this.transaction = transaction;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public FlagType getType() {
    return type;
  }

  public void setType(FlagType type) {
    this.type = type;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public FlagStatus getStatus() {
    return status;
  }

  public void setStatus(FlagStatus status) {
    this.status = status;
  }

  public Instant getSlaDeadline() {
    return slaDeadline;
  }

  public void setSlaDeadline(Instant slaDeadline) {
    this.slaDeadline = slaDeadline;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public User getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(User reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public String getReviewNote() {
    return reviewNote;
  }

  public void setReviewNote(String reviewNote) {
    this.reviewNote = reviewNote;
  }
}
