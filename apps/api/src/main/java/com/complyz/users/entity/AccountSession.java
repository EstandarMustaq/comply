package com.complyz.users.entity;

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

    protected AccountSession() {}

    public AccountSession(UUID accountId, UUID externalIdentityId, String idpSessionId,
                          Instant createdAt, Instant expiresAt) {
        this.accountId = Objects.requireNonNull(accountId);
        this.externalIdentityId = Objects.requireNonNull(externalIdentityId);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        if (!expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("Session expiry must follow creation");
        if (idpSessionId == null || idpSessionId.isBlank()) throw new IllegalArgumentException("Missing IdP session identifier");
        try {
            this.sessionFingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(idpSessionId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public void revoke(Instant at) {
        Objects.requireNonNull(at);
        if (at.isBefore(createdAt)) throw new IllegalArgumentException("Revocation precedes session creation");
        if (revokedAt == null) revokedAt = at;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public UUID getExternalIdentityId() { return externalIdentityId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public long getVersion() { return version; }
}
