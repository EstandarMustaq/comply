package com.complyz.users.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Identity & Access aggregate foundation. Credentials remain in the legacy/IdP lifecycle. */
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

    protected Account() {}

    public Account(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    public UUID getId() { return id; }
    public UUID getLegacyUserId() { return legacyUserId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
