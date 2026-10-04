package com.complyz.monitoring.service;

import com.complyz.clients.entity.Client;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.monitoring.entity.ThresholdRule;
import com.complyz.monitoring.entity.ThresholdRuleType;
import com.complyz.monitoring.entity.Transaction;
import com.complyz.monitoring.entity.TransactionType;
import com.complyz.monitoring.repository.ThresholdRuleRepository;
import com.complyz.monitoring.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class StructuringDetectionService {

    private static final List<TransactionType> CASH_TRANSACTION_TYPES =
            List.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL);

    private final TransactionRepository transactionRepository;
    private final ThresholdRuleRepository ruleRepository;
    private final FlagRepository flagRepository;

    public StructuringDetectionService(
            TransactionRepository transactionRepository,
            ThresholdRuleRepository ruleRepository,
            FlagRepository flagRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
        this.flagRepository = flagRepository;
    }

    @Transactional
    public Optional<Flag> detectStructuring(Transaction currentTx) {
        if (currentTx == null || currentTx.getClient() == null) return Optional.empty();
        if (!CASH_TRANSACTION_TYPES.contains(currentTx.getType())) return Optional.empty();

        Client client = currentTx.getClient();
        if (client.getOrganization() == null || client.getOrganization().getId() == null) {
            throw new IllegalStateException("A organização do cliente é obrigatória para avaliar a operação");
        }
        var organizationId = client.getOrganization().getId();
        List<ThresholdRule> structuringRules = ruleRepository.findByTypeAndActiveTrue(ThresholdRuleType.STRUCTURING);
        if (structuringRules.isEmpty()) return Optional.empty();

        ThresholdRule rule = structuringRules.get(0);
        int windowDays = rule.getAggregationWindowDays() != null ? rule.getAggregationWindowDays() : 30;
        LocalDate startDate = currentTx.getTransactionDate().minusDays(Math.max(windowDays - 1, 0));
        LocalDate endDate = currentTx.getTransactionDate();

        List<Transaction> windowTransactions = transactionRepository.findByClientAndOrganizationAndDateBetweenAndTypeIn(
                client.getId(), organizationId, startDate, endDate, CASH_TRANSACTION_TYPES);

        // Require at least three transactions in the configured window.
        boolean allBelowThreshold = rule.getValueThreshold() != null && windowTransactions.stream()
                .allMatch(transaction -> transaction.getAmount().compareTo(rule.getValueThreshold()) < 0);
        if (windowTransactions.size() >= 3 && allBelowThreshold
                && flagRepository.countByTransactionIdAndOrganizationIdAndType(
                        currentTx.getId(), organizationId, FlagType.STRUCTURING_SUSPECTED) == 0) {
            BigDecimal sum = windowTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (rule.getValueThreshold() != null && sum.compareTo(rule.getValueThreshold()) >= 0) {
                String reason = String.format(
                        "Suspeita de fraccionamento de operações (structuring): identificadas %d operações no período de %d dias " +
                        "com montante cumulativo de %.2f MT (limiar de detecção: %.2f MT).",
                        windowTransactions.size(), windowDays, sum, rule.getValueThreshold()
                );

                Flag flag = new Flag(client, currentTx, FlagType.STRUCTURING_SUSPECTED, reason, null);
                return Optional.of(flagRepository.save(flag));
            }
        }

        return Optional.empty();
    }
}
