package com.complyz.clients.service;

import com.complyz.clients.entity.BeneficialOwner;
import com.complyz.clients.entity.BeneficialOwnerRole;
import com.complyz.clients.entity.Client;
import com.complyz.clients.entity.ClientType;
import com.complyz.clients.repository.BeneficialOwnerRepository;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock ClientRepository clientRepository;
    @Mock BeneficialOwnerRepository ownerRepository;
    @Mock FlagRepository flagRepository;

    private KycService service;

    @BeforeEach
    void setUp() {
        service = new KycService(clientRepository, ownerRepository, flagRepository, new TenantAccessService());
    }

    @Test
    void individualWithRequiredIdentityIsComplete() {
        Client client = client("BI", ClientType.INDIVIDUAL);
        when(ownerRepository.findByClientIdAndClientOrganizationId(
                client.getId(), client.getOrganization().getId())).thenReturn(List.of());

        assertThat(service.isComplete(client)).isTrue();
    }

    @Test
    void corporateRequiresVerifiedBeneficialOwnerWithTenPercentOrMore() {
        Client client = client("NUIT", ClientType.CORPORATE);
        BeneficialOwner owner = new BeneficialOwner(client, "Owner", BeneficialOwnerRole.CONTROLLING_PERSON,
                new BigDecimal("25"));
        when(ownerRepository.findByClientIdAndClientOrganizationId(
                client.getId(), client.getOrganization().getId())).thenReturn(List.of(owner));

        assertThat(service.isComplete(client)).isFalse();
        owner.setVerifiedAt(LocalDate.now());
        assertThat(service.isComplete(client)).isTrue();
    }

    @Test
    void incompleteKycPersistsFlagWithoutDuplicatingPendingFlag() {
        Client client = client("BI", ClientType.INDIVIDUAL);
        client.setId(UUID.randomUUID());
        client.setIdDocumentNumber("");
        when(clientRepository.findByIdAndOrganization(client.getId(), client.getOrganization())
        ).thenReturn(java.util.Optional.of(client));
        when(flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                client.getId(), client.getOrganization().getId(), FlagType.KYC_INCOMPLETE,
                FlagStatus.PENDING)).thenReturn(0L);
        when(clientRepository.save(client)).thenReturn(client);

        service.recalculateAndPersist(client.getId(), actor(client.getOrganization()));

        assertThat(client.isKycComplete()).isFalse();
        verify(flagRepository).save(any());
    }

    @Test
    void foreignClientCannotBeRecalculatedOrReceiveBeneficialOwners() {
        Organization organization = new Organization("actor-org", "Actor organization");
        organization.setId(UUID.randomUUID());
        User actor = actor(organization);
        UUID foreignClientId = UUID.randomUUID();
        when(clientRepository.findByIdAndOrganization(foreignClientId, organization))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.recalculateAndPersist(foreignClientId, actor))
                .isInstanceOf(IllegalArgumentException.class);

        verify(clientRepository).findByIdAndOrganization(foreignClientId, organization);
        verifyNoInteractions(ownerRepository, flagRepository);
    }

    @Test
    void foreignBeneficialOwnerCannotBeVerified() {
        Organization organization = new Organization("actor-org", "Actor organization");
        organization.setId(UUID.randomUUID());
        User actor = actor(organization);
        UUID foreignOwnerId = UUID.randomUUID();
        when(ownerRepository.findByIdAndClientOrganizationId(foreignOwnerId, organization.getId()))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.verifyOwner(foreignOwnerId, actor))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ownerRepository).findByIdAndClientOrganizationId(foreignOwnerId, organization.getId());
        verifyNoInteractions(clientRepository, flagRepository);
    }

    @Test
    void ownerListIsFilteredByTheAuthenticatedOrganization() {
        Organization organization = new Organization("actor-org", "Actor organization");
        organization.setId(UUID.randomUUID());
        User actor = actor(organization);
        UUID foreignClientId = UUID.randomUUID();
        when(ownerRepository.findByClientIdAndClientOrganizationId(foreignClientId, organization.getId()))
                .thenReturn(List.of());

        assertThat(service.listOwners(foreignClientId, actor)).isEmpty();

        verify(ownerRepository).findByClientIdAndClientOrganizationId(foreignClientId, organization.getId());
        verifyNoInteractions(clientRepository, flagRepository);
    }

    private Client client(String documentType, ClientType type) {
        Client client = new Client("Cliente", documentType, "123");
        client.setId(UUID.randomUUID());
        client.setClientType(type);
        Organization organization = new Organization("test-org", "Test Organization");
        organization.setId(UUID.randomUUID());
        client.setOrganization(organization);
        return client;
    }

    private static User actor(Organization organization) {
        User actor = new User();
        actor.setOrganization(organization);
        return actor;
    }
}
