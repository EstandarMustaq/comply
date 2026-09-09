package com.fincomp.clients.service;

import com.fincomp.clients.entity.BeneficialOwner;
import com.fincomp.clients.entity.BeneficialOwnerRole;
import com.fincomp.clients.entity.Client;
import com.fincomp.clients.entity.ClientType;
import com.fincomp.clients.repository.BeneficialOwnerRepository;
import com.fincomp.clients.repository.ClientRepository;
import com.fincomp.flags.entity.FlagStatus;
import com.fincomp.flags.entity.FlagType;
import com.fincomp.flags.repository.FlagRepository;
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
        service = new KycService(clientRepository, ownerRepository, flagRepository);
    }

    @Test
    void individualWithRequiredIdentityIsComplete() {
        Client client = client("BI", ClientType.INDIVIDUAL);
        when(ownerRepository.findByClientId(client.getId())).thenReturn(List.of());

        assertThat(service.isComplete(client)).isTrue();
    }

    @Test
    void corporateRequiresVerifiedBeneficialOwnerWithTenPercentOrMore() {
        Client client = client("NUIT", ClientType.CORPORATE);
        BeneficialOwner owner = new BeneficialOwner(client, "Owner", BeneficialOwnerRole.CONTROLLING_PERSON,
                new BigDecimal("25"));
        when(ownerRepository.findByClientId(client.getId())).thenReturn(List.of(owner));

        assertThat(service.isComplete(client)).isFalse();
        owner.setVerifiedAt(LocalDate.now());
        assertThat(service.isComplete(client)).isTrue();
    }

    @Test
    void incompleteKycPersistsFlagWithoutDuplicatingPendingFlag() {
        Client client = client("BI", ClientType.INDIVIDUAL);
        client.setId(UUID.randomUUID());
        client.setIdDocumentNumber("");
        when(clientRepository.findById(client.getId())).thenReturn(java.util.Optional.of(client));
        when(flagRepository.existsByClientIdAndTypeAndStatus(
                client.getId(), FlagType.KYC_INCOMPLETE, FlagStatus.PENDING)).thenReturn(false);
        when(clientRepository.save(client)).thenReturn(client);

        service.recalculateAndPersist(client.getId());

        assertThat(client.isKycComplete()).isFalse();
        verify(flagRepository).save(any());
    }

    private Client client(String documentType, ClientType type) {
        Client client = new Client("Cliente", documentType, "123");
        client.setId(UUID.randomUUID());
        client.setClientType(type);
        return client;
    }
}
