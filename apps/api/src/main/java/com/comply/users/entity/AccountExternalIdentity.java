package com.comply.users.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Identity &amp; Access lifecycle metadata. Provider secrets and tokens never belong here. */
@Entity
@Table(name = "account_external_identities")
public class AccountExternalIdentity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "issuer", nullable = false, updatable = false, length = 255)
  private String issuer;

  @Column(name = "subject", nullable = false, updatable = false, length = 255)
  private String subject;

  @Column(name = "linked_at", nullable = false, updatable = false)
  private Instant linkedAt;

  @Column(name = "unlinked_at")
  private Instant unlinkedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Required by JPA to hydrate persisted records. */
  protected AccountExternalIdentity() {}

  /**
   * Links an Account to an exact, case-sensitive issuer/subject pair. The database reserves this
   * pair globally even after unlinking.
   *
   * @param accountId Account UUID; repository callers must obtain it from authenticated identity
   *     context
   * @param issuer nonblank exact issuer reference, at most 255 characters without surrounding
   *     whitespace
   * @param subject nonblank exact subject reference, at most 255 characters without surrounding
   *     whitespace
   * @param linkedAt non-null link start time
   * @throws NullPointerException if a required UUID or timestamp is null
   * @throws IllegalArgumentException if issuer or subject is null, blank, padded or longer than 255
   *     characters
   */
  public AccountExternalIdentity(UUID accountId, String issuer, String subject, Instant linkedAt) {
    this.accountId = Objects.requireNonNull(accountId);
    if (issuer == null
        || issuer.isBlank()
        || !issuer.equals(issuer.strip())
        || issuer.length() > 255) {
      throw new IllegalArgumentException("Invalid issuer reference");
    }
    this.issuer = issuer;
    if (subject == null
        || subject.isBlank()
        || !subject.equals(subject.strip())
        || subject.length() > 255) {
      throw new IllegalArgumentException("Invalid subject reference");
    }
    this.subject = subject;
    this.linkedAt = Objects.requireNonNull(linkedAt);
  }

  /**
   * Records the first unlink time, retaining the identity and dependent lifecycle history.
   * Subsequent valid calls preserve that time; unlinking excludes dependent factors and sessions
   * from active queries.
   *
   * @param at non-null terminal time at or after this record's lifecycle start
   * @throws NullPointerException if at is null
   * @throws IllegalArgumentException if at precedes the lifecycle start
   */
  public void unlink(Instant at) {
    Objects.requireNonNull(at);
    if (at.isBefore(linkedAt))
      throw new IllegalArgumentException("Lifecycle end precedes creation");
    if (unlinkedAt == null) unlinkedAt = at;
  }

  /** {@return the persistent record UUID, or {@code null} before persistence assigns it} */
  public UUID getId() {
    return id;
  }

  /** {@return the referenced Account UUID, independent of Organization membership} */
  public UUID getAccountId() {
    return accountId;
  }

  /** {@return the exact issuer reference, preserving case and without surrounding whitespace} */
  public String getIssuer() {
    return issuer;
  }

  /** {@return the exact subject reference, meaningful only with its issuer} */
  public String getSubject() {
    return subject;
  }

  /** {@return the link start time used by active identity and dependent-record queries} */
  public Instant getLinkedAt() {
    return linkedAt;
  }

  /** {@return the first unlink time, or {@code null} while the link has not been ended} */
  public Instant getUnlinkedAt() {
    return unlinkedAt;
  }

  /** {@return the persistence version used to reject stale concurrent updates} */
  public long getVersion() {
    return version;
  }
}
