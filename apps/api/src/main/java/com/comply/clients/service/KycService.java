package com.comply.clients.service;

import com.comply.clients.entity.BeneficialOwner;
import com.comply.clients.entity.BeneficialOwnerRole;
import com.comply.clients.entity.Client;
import com.comply.clients.entity.ClientType;
import com.comply.clients.repository.BeneficialOwnerRepository;
import com.comply.clients.repository.ClientRepository;
import com.comply.flags.entity.Flag;
import com.comply.flags.entity.FlagStatus;
import com.comply.flags.entity.FlagType;
import com.comply.flags.repository.FlagRepository;
import com.comply.tenancy.service.TenantAccessService;
import com.comply.users.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KycService {

  private final ClientRepository clientRepository;
  private final BeneficialOwnerRepository beneficialOwnerRepository;
  private final FlagRepository flagRepository;
  private final TenantAccessService tenantAccessService;

  public KycService(
      ClientRepository clientRepository,
      BeneficialOwnerRepository beneficialOwnerRepository,
      FlagRepository flagRepository,
      TenantAccessService tenantAccessService) {
    this.clientRepository = clientRepository;
    this.beneficialOwnerRepository = beneficialOwnerRepository;
    this.flagRepository = flagRepository;
    this.tenantAccessService = tenantAccessService;
  }

  @Transactional(readOnly = true)
  public boolean isComplete(Client client) {
    if (client == null
        || isBlank(client.getFullName())
        || isBlank(client.getIdDocumentType())
        || isBlank(client.getIdDocumentNumber())) {
      return false;
    }

    UUID organizationId =
        client.getOrganization() == null ? null : client.getOrganization().getId();
    if (organizationId == null) {
      return false;
    }
    List<BeneficialOwner> owners =
        beneficialOwnerRepository.findByClientIdAndClientOrganizationId(
            client.getId(), organizationId);
    if (isCorporate(client)) {
      return !owners.isEmpty()
          && owners.stream().allMatch(this::isValidOwner)
          && owners.stream()
              .anyMatch(
                  owner ->
                      owner.getOwnershipPercentage() != null
                          && owner.getOwnershipPercentage().compareTo(BigDecimal.TEN) >= 0);
    }
    return true;
  }

  @Transactional
  public Client recalculateAndPersist(UUID clientId, User actor) {
    Client client = findClient(clientId, actor);
    boolean complete = isComplete(client);
    client.setKycComplete(complete);
    Client saved = clientRepository.save(client);

    if (!complete
        && flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                clientId,
                client.getOrganization().getId(),
                FlagType.KYC_INCOMPLETE,
                FlagStatus.PENDING)
            == 0) {
      flagRepository.save(
          new Flag(
              client,
              null,
              FlagType.KYC_INCOMPLETE,
              "Customer review is incomplete: identification or beneficial owner details are missing.",
              null));
    }
    return saved;
  }

  @Transactional
  public List<BeneficialOwner> listOwners(UUID clientId, User actor) {
    UUID organizationId = requireOrganizationId(actor);
    return beneficialOwnerRepository.findByClientIdAndClientOrganizationId(
        clientId, organizationId);
  }

  @Transactional
  public BeneficialOwner addOwner(
      UUID clientId,
      String fullName,
      BeneficialOwnerRole role,
      BigDecimal ownershipPercentage,
      User actor) {
    Client client = findClient(clientId, actor);
    validateOwner(fullName, role, ownershipPercentage);
    BeneficialOwner owner = new BeneficialOwner(client, fullName.trim(), role, ownershipPercentage);
    return beneficialOwnerRepository.save(owner);
  }

  @Transactional
  public BeneficialOwner verifyOwner(UUID ownerId, User actor) {
    UUID organizationId = requireOrganizationId(actor);
    BeneficialOwner owner =
        beneficialOwnerRepository
            .findByIdAndClientOrganizationId(ownerId, organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Beneficial owner not found: " + ownerId));
    owner.setVerifiedAt(LocalDate.now());
    BeneficialOwner saved = beneficialOwnerRepository.save(owner);
    recalculateAndPersist(owner.getClient().getId(), actor);
    return saved;
  }

  private Client findClient(UUID clientId, User actor) {
    var organization = tenantAccessService.requireOrganization(actor);
    if (organization.getId() == null) {
      throw new IllegalStateException("The user's Organization is required.");
    }
    return clientRepository
        .findByIdAndOrganization(clientId, organization)
        .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + clientId));
  }

  private UUID requireOrganizationId(User actor) {
    UUID organizationId = tenantAccessService.organizationId(actor);
    if (organizationId == null) {
      throw new IllegalStateException("The user's Organization is required.");
    }
    return organizationId;
  }

  private boolean isCorporate(Client client) {
    return client.getClientType() == ClientType.CORPORATE;
  }

  private boolean isValidOwner(BeneficialOwner owner) {
    return !isBlank(owner.getFullName())
        && owner.getRole() != null
        && owner.getVerifiedAt() != null
        && (owner.getOwnershipPercentage() == null
            || (owner.getOwnershipPercentage().compareTo(BigDecimal.ZERO) >= 0
                && owner.getOwnershipPercentage().compareTo(new BigDecimal("100")) <= 0));
  }

  private void validateOwner(String fullName, BeneficialOwnerRole role, BigDecimal percentage) {
    if (isBlank(fullName) || role == null) {
      throw new IllegalArgumentException("Beneficial owner name and role are required.");
    }
    if (percentage != null
        && (percentage.compareTo(BigDecimal.ZERO) < 0
            || percentage.compareTo(new BigDecimal("100")) > 0)) {
      throw new IllegalArgumentException("Ownership percentage must be between 0 and 100.");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
