package com.complyz.compliance.service;

import com.complyz.clients.entity.Client;
import com.complyz.clients.entity.DueDiligenceRegime;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.monitoring.entity.Transaction;
import com.complyz.monitoring.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
public class DueDiligenceRegimeValidator {

    private final TransactionRepository transactionRepository;
    private final FlagRepository flagRepository;
    private final ClientRepository clientRepository;

    @Value("${complyz.compliance.simplified-regime.monthly-limit:30000.00}")
    private BigDecimal monthlyLimit;

    @Value("${complyz.compliance.simplified-regime.annual-limit:240000.00}")
    private BigDecimal annualLimit;

    public DueDiligenceRegimeValidator(
            TransactionRepository transactionRepository,
            FlagRepository flagRepository,
            ClientRepository clientRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.flagRepository = flagRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Optional<Flag> validateClientRegimeCompliance(Client client, Transaction newTransaction) {
        if (client == null || client.getDueDiligenceRegime() != DueDiligenceRegime.SIMPLIFIED) {
            return Optional.empty();
        }

        LocalDate txDate = newTransaction != null ? newTransaction.getTransactionDate() : LocalDate.now();
        LocalDate startOfMonth = txDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = txDate.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate startOfYear = txDate.with(TemporalAdjusters.firstDayOfYear());
        LocalDate endOfYear = txDate.with(TemporalAdjusters.lastDayOfYear());

        BigDecimal transactionAmount = newTransaction != null && newTransaction.getAmount() != null
                ? newTransaction.getAmount()
                : BigDecimal.ZERO;
        if (client.getOrganization() == null || client.getOrganization().getId() == null) {
            throw new IllegalStateException("The customer's Organization is required to validate this due diligence regime.");
        }
        BigDecimal monthlyTotal = transactionRepository.sumAmountByClientAndOrganizationAndDateBetween(
                client.getId(), client.getOrganization().getId(), startOfMonth, endOfMonth);
        BigDecimal annualTotal = transactionRepository.sumAmountByClientAndOrganizationAndDateBetween(
                client.getId(), client.getOrganization().getId(), startOfYear, endOfYear);
        if (newTransaction != null && newTransaction.getId() == null) {
            monthlyTotal = monthlyTotal.add(transactionAmount);
            annualTotal = annualTotal.add(transactionAmount);
        }

        boolean monthlyExceeded = monthlyTotal.compareTo(monthlyLimit) > 0;
        boolean annualExceeded = annualTotal.compareTo(annualLimit) > 0;
        if (monthlyExceeded || annualExceeded) {
            client.setDueDiligenceRegime(DueDiligenceRegime.NORMAL);
            client.setKycComplete(false);
            clientRepository.save(client);

            String exceededLimit = monthlyExceeded
                    ? String.format("monthly limit of %.2f MT (month-to-date total: %.2f MT)", monthlyLimit, monthlyTotal)
                    : String.format("annual limit of %.2f MT (year-to-date total: %.2f MT)", annualLimit, annualTotal);
            String reason = String.format(
                    "The customer exceeded the simplified due diligence %s. " +
                    "The system moved the customer to standard due diligence and marked the review incomplete. " +
                    "Complete the risk profile and due diligence review.",
                    exceededLimit
            );

            if (flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                    client.getId(), client.getOrganization().getId(), FlagType.KYC_INCOMPLETE,
                    FlagStatus.PENDING) == 0) {
                Flag flag = new Flag(client, newTransaction, FlagType.KYC_INCOMPLETE, reason, null);
                return Optional.of(flagRepository.save(flag));
            }
        }

        return Optional.empty();
    }
}
