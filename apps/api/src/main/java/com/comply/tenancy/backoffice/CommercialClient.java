package com.comply.tenancy.backoffice;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Segregated platform reference. No members, tenant navigation, policies or authorization
 * semantics.
 */
@Entity
@Table(name = "commercial_clients", schema = "platform_backoffice")
public class CommercialClient {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "contract_reference", nullable = false)
  private String contractReference;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CommercialClientStatus status;

  protected CommercialClient() {}

  /**
   * Creates commercial metadata without granting tenant access or changing Organization lifecycle.
   *
   * @param name display name
   * @param contractReference commercial contract reference
   * @param status commercial metadata status, with no tenant authorization effect
   */
  public CommercialClient(String name, String contractReference, CommercialClientStatus status) {
    this.name = name;
    this.contractReference = contractReference;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getContractReference() {
    return contractReference;
  }

  public CommercialClientStatus getStatus() {
    return status;
  }
}
