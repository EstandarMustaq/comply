package com.complyz.users.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Identity & Access lifecycle metadata. Provider secrets and tokens never belong here. */
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

    public AccountFactorReference(UUID accountId, UUID externalIdentityId, String providerFactorId, Instant registeredAt) {
        this.accountId = Objects.requireNonNull(accountId);
        this.externalIdentityId = Objects.requireNonNull(externalIdentityId);
        if (providerFactorId == null || providerFactorId.isBlank() || !providerFactorId.equals(providerFactorId.strip()) || providerFactorId.length() > 255) {
            throw new IllegalArgumentException("Invalid providerFactorId reference");
        }
        this.providerFactorId = providerFactorId;
        this.registeredAt = Objects.requireNonNull(registeredAt);
    }

    public void revoke(Instant at) {
        Objects.requireNonNull(at);
        if (at.isBefore(registeredAt)) throw new IllegalArgumentException("Lifecycle end precedes creation");
        if (revokedAt == null) revokedAt = at;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getProviderFactorId() { return providerFactorId; }
    public UUID getExternalIdentityId() { return externalIdentityId; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public long getVersion() { return version; }
}
