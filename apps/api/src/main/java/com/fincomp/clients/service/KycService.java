package com.fincomp.clients.service;

import com.fincomp.clients.entity.BeneficialOwner;
import com.fincomp.clients.entity.BeneficialOwnerRole;
import com.fincomp.clients.entity.Client;
import com.fincomp.clients.entity.ClientType;
import com.fincomp.clients.repository.BeneficialOwnerRepository;
import com.fincomp.clients.repository.ClientRepository;
import com.fincomp.flags.entity.Flag;
import com.fincomp.flags.entity.FlagStatus;
import com.fincomp.flags.entity.FlagType;
import com.fincomp.flags.repository.FlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class KycService {

    private final ClientRepository clientRepository;
    private final BeneficialOwnerRepository beneficialOwnerRepository;
    private final FlagRepository flagRepository;

    public KycService(
            ClientRepository clientRepository,
            BeneficialOwnerRepository beneficialOwnerRepository,
            FlagRepository flagRepository
    ) {
        this.clientRepository = clientRepository;
        this.beneficialOwnerRepository = beneficialOwnerRepository;
        this.flagRepository = flagRepository;
    }

    @Transactional(readOnly = true)
    public boolean isComplete(Client client) {
        if (client == null || isBlank(client.getFullName())
                || isBlank(client.getIdDocumentType())
                || isBlank(client.getIdDocumentNumber())) {
            return false;
        }

        List<BeneficialOwner> owners = beneficialOwnerRepository.findByClientId(client.getId());
        if (isCorporate(client)) {
            return !owners.isEmpty() && owners.stream().allMatch(this::isValidOwner)
                    && owners.stream().anyMatch(owner ->
                    owner.getOwnershipPercentage() != null
                            && owner.getOwnershipPercentage().compareTo(BigDecimal.TEN) >= 0);
        }
        return true;
    }

    @Transactional
    public Client recalculateAndPersist(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + clientId));
        boolean complete = isComplete(client);
        client.setKycComplete(complete);
        Client saved = clientRepository.save(client);

        if (!complete && !flagRepository.existsByClientIdAndTypeAndStatus(
                clientId, FlagType.KYC_INCOMPLETE, FlagStatus.PENDING)) {
            flagRepository.save(new Flag(client, null, FlagType.KYC_INCOMPLETE,
                    "KYC incompleto: identificação obrigatória ou beneficiários efetivos pendentes.",
                    null));
        }
        return saved;
    }

    @Transactional
    public BeneficialOwner addOwner(UUID clientId, String fullName, BeneficialOwnerRole role,
                                    BigDecimal ownershipPercentage) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + clientId));
        validateOwner(fullName, role, ownershipPercentage);
        BeneficialOwner owner = new BeneficialOwner(client, fullName.trim(), role, ownershipPercentage);
        return beneficialOwnerRepository.save(owner);
    }

    @Transactional
    public BeneficialOwner verifyOwner(UUID ownerId) {
        BeneficialOwner owner = beneficialOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiário efetivo não encontrado: " + ownerId));
        owner.setVerifiedAt(LocalDate.now());
        BeneficialOwner saved = beneficialOwnerRepository.save(owner);
        recalculateAndPersist(owner.getClient().getId());
        return saved;
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
            throw new IllegalArgumentException("Nome e função do beneficiário efetivo são obrigatórios");
        }
        if (percentage != null && (percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("Percentagem de participação deve estar entre 0 e 100");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
