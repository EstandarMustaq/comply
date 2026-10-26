package com.comply.tenancy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Owned by Organization. References Account by identity without cross-context entity writes. */
@Entity
@Table(
    name = "organization_memberships",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_membership_organization_account",
            columnNames = {"organization_id", "account_id"}))
public class OrganizationMembership {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private OrganizationRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MembershipStatus status = MembershipStatus.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Version
  @Column(nullable = false)
  private long version;

  protected OrganizationMembership() {}

  /**
   * Creates an active membership with a role scoped to the specified Organization.
   *
   * @param organizationId Organization UUID defining the membership scope
   * @param accountId target Account UUID within the authorized Organization scope
   * @param role role within the specified Organization
   */
  public OrganizationMembership(UUID organizationId, UUID accountId, OrganizationRole role) {
    this.organizationId = organizationId;
    this.accountId = accountId;
    this.role = role;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public OrganizationRole getRole() {
    return role;
  }

  public MembershipStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }
}
