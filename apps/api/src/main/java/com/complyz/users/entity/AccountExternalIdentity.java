package com.complyz.users.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Identity & Access lifecycle metadata. Provider secrets and tokens never belong here. */
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

    protected AccountExternalIdentity() {}

    public AccountExternalIdentity(UUID accountId, String issuer, String subject, Instant linkedAt) {
        this.accountId = Objects.requireNonNull(accountId);
        if (issuer == null || issuer.isBlank() || !issuer.equals(issuer.strip()) || issuer.length() > 255) {
            throw new IllegalArgumentException("Invalid issuer reference");
        }
        this.issuer = issuer;
        if (subject == null || subject.isBlank() || !subject.equals(subject.strip()) || subject.length() > 255) {
            throw new IllegalArgumentException("Invalid subject reference");
        }
        this.subject = subject;
        this.linkedAt = Objects.requireNonNull(linkedAt);
    }

    public void unlink(Instant at) {
        Objects.requireNonNull(at);
        if (at.isBefore(linkedAt)) throw new IllegalArgumentException("Lifecycle end precedes creation");
        if (unlinkedAt == null) unlinkedAt = at;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getIssuer() { return issuer; }
    public String getSubject() { return subject; }
    public Instant getLinkedAt() { return linkedAt; }
    public Instant getUnlinkedAt() { return unlinkedAt; }
    public long getVersion() { return version; }
}
