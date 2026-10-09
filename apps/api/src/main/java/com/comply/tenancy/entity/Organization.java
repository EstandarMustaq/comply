package com.comply.tenancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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

  /**
   * Creates a tenant with no commercial reference until reviewed mapping is available.
   *
   * @param slug unique tenant slug
   * @param name display name
   */
  public Organization(String slug, String name) {
    this.slug = slug;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  /**
   * Assigns the Organization UUID used as the tenant boundary.
   *
   * @param id persistent record UUID
   */
  public void setId(UUID id) {
    this.id = id;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }

  @JsonIgnore
  public UUID getCommercialClientId() {
    return commercialClientId;
  }

  /**
   * Sets the scalar commercial reference without changing memberships or tenant authorization.
   *
   * @param commercialClientId back-office reference UUID, or null while unresolved
   */
  public void setCommercialClientId(UUID commercialClientId) {
    this.commercialClientId = commercialClientId;
  }

  @JsonIgnore
  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
