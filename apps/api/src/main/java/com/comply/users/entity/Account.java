package com.comply.users.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Identity &amp; Access aggregate foundation. Credentials remain in the legacy/IdP lifecycle. */
@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "legacy_user_id", unique = true, updatable = false)
  private UUID legacyUserId;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Version
  @Column(nullable = false)
  private long version;

  /** Required by JPA to hydrate persisted records. */
  protected Account() {}

  /**
   * Creates a global identity profile without credentials or Organization membership.
   *
   * @param fullName Account profile name
   * @param email unique Account email
   */
  public Account(String fullName, String email) {
    this.fullName = fullName;
    this.email = email;
  }

  /** {@return the persistent record UUID, or {@code null} before persistence assigns it} */
  public UUID getId() {
    return id;
  }

  /** {@return the preserved legacy User UUID, or {@code null} for a newly created Account} */
  public UUID getLegacyUserId() {
    return legacyUserId;
  }

  /** {@return the Account profile name} */
  public String getFullName() {
    return fullName;
  }

  /**
   * {@return the Account email; external identities must be linked by verified issuer/subject
   * claims}
   */
  public String getEmail() {
    return email;
  }

  /** {@return the record creation time, preserved from legacy data during backfill} */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** {@return the persistence version used to reject stale concurrent updates} */
  public long getVersion() {
    return version;
  }
}
