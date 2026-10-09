package com.comply.users.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Session metadata only. IdP/BFF tokens are never persisted in this record. */
@Entity
@Table(name = "account_sessions")
public class AccountSession {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "external_identity_id", nullable = false, updatable = false)
  private UUID externalIdentityId;

  @JsonIgnore
  @Column(name = "session_fingerprint", nullable = false, updatable = false, length = 64)
  private String sessionFingerprint;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Required by JPA to hydrate persisted records. */
  protected AccountSession() {}

  /**
   * Creates session metadata and stores only the SHA-256 fingerprint of the IdP session identifier.
   * The database requires the external identity to belong to the same Account; expiry must follow
   * creation.
   *
   * @param accountId Account UUID; repository callers must obtain it from authenticated identity
   *     context
   * @param externalIdentityId external identity UUID belonging to the same Account
   * @param idpSessionId nonblank provider session identifier, hashed immediately and never stored
   *     raw
   * @param createdAt non-null session creation time
   * @param expiresAt non-null expiry strictly after creation
   * @throws NullPointerException if a required UUID or timestamp is null
   * @throws IllegalArgumentException if the session identifier is null or blank, or expiry does not
   *     follow creation
   */
  public AccountSession(
      UUID accountId,
      UUID externalIdentityId,
      String idpSessionId,
      Instant createdAt,
      Instant expiresAt) {
    this.accountId = Objects.requireNonNull(accountId);
    this.externalIdentityId = Objects.requireNonNull(externalIdentityId);
    this.createdAt = Objects.requireNonNull(createdAt);
    this.expiresAt = Objects.requireNonNull(expiresAt);
    if (!expiresAt.isAfter(createdAt))
      throw new IllegalArgumentException("Session expiry must follow creation");
    if (idpSessionId == null || idpSessionId.isBlank())
      throw new IllegalArgumentException("Missing IdP session identifier");
    try {
      this.sessionFingerprint =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(idpSessionId.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
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
    if (at.isBefore(createdAt))
      throw new IllegalArgumentException("Revocation precedes session creation");
    if (revokedAt == null) revokedAt = at;
  }

  /** {@return the persistent record UUID, or {@code null} before persistence assigns it} */
  public UUID getId() {
    return id;
  }

  /** {@return the referenced Account UUID, independent of Organization membership} */
  public UUID getAccountId() {
    return accountId;
  }

  /** {@return the external identity UUID constrained to belong to this record's Account} */
  public UUID getExternalIdentityId() {
    return externalIdentityId;
  }

  /** {@return the inclusive session start time used by active queries} */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** {@return the exclusive expiry boundary used by active session queries} */
  public Instant getExpiresAt() {
    return expiresAt;
  }

  /** {@return the first revocation time, or {@code null} while no revocation has been recorded} */
  public Instant getRevokedAt() {
    return revokedAt;
  }

  /** {@return the persistence version used to reject stale concurrent updates} */
  public long getVersion() {
    return version;
  }
}
