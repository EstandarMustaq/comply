package com.comply.users.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Identity &amp; Access lifecycle metadata. Provider secrets and tokens never belong here. */
@Entity
@Table(name = "account_factor_references")
public class AccountFactorReference {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "external_identity_id", nullable = false, updatable = false)
  private UUID externalIdentityId;

  @Column(name = "provider_factor_id", nullable = false, updatable = false, length = 255)
  private String providerFactorId;

  @Column(name = "registered_at", nullable = false, updatable = false)
  private Instant registeredAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected AccountFactorReference() {}

  /**
   * Registers a non-secret provider factor reference. The database requires the external identity
   * to belong to the same Account.
   *
   * @param accountId Account UUID; repository callers must obtain it from authenticated identity
   *     context
   * @param externalIdentityId external identity UUID belonging to the same Account
   * @param providerFactorId non-secret, nonblank factor reference, at most 255 characters without
   *     surrounding whitespace
   * @param registeredAt non-null factor registration time
   * @throws NullPointerException if a required UUID or timestamp is null
   * @throws IllegalArgumentException if the provider factor reference is null, blank, padded or
   *     longer than 255 characters
   */
  public AccountFactorReference(
      UUID accountId, UUID externalIdentityId, String providerFactorId, Instant registeredAt) {
    this.accountId = Objects.requireNonNull(accountId);
    this.externalIdentityId = Objects.requireNonNull(externalIdentityId);
    if (providerFactorId == null
        || providerFactorId.isBlank()
        || !providerFactorId.equals(providerFactorId.strip())
        || providerFactorId.length() > 255) {
      throw new IllegalArgumentException("Invalid providerFactorId reference");
    }
    this.providerFactorId = providerFactorId;
    this.registeredAt = Objects.requireNonNull(registeredAt);
  }

  /**
   * Records the first revocation time without deleting lifecycle history. Subsequent valid calls
   * preserve that time; other lifecycle records are unaffected.
   *
   * @param at non-null terminal time at or after this record's lifecycle start
   * @throws NullPointerException if at is null
   * @throws IllegalArgumentException if at precedes the lifecycle start
   */
  public void revoke(Instant at) {
    Objects.requireNonNull(at);
    if (at.isBefore(registeredAt))
      throw new IllegalArgumentException("Lifecycle end precedes creation");
    if (revokedAt == null) revokedAt = at;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public String getProviderFactorId() {
    return providerFactorId;
  }

  public UUID getExternalIdentityId() {
    return externalIdentityId;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public long getVersion() {
    return version;
  }
}
