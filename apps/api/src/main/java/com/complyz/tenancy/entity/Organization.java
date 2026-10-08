package com.complyz.tenancy.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Scalar back-office reference: never traverse it to authorize customer requests.
    @JsonIgnore
    @Column(name = "commercial_client_id")
    private UUID commercialClientId;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    public Organization() {}

    public Organization(String slug, String name) {
        this.slug = slug;
        this.name = name;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    @JsonIgnore
    public UUID getCommercialClientId() { return commercialClientId; }
    public void setCommercialClientId(UUID commercialClientId) { this.commercialClientId = commercialClientId; }
    @JsonIgnore
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
