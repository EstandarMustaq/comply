package com.fincomp.compliance.service;

import com.fincomp.clients.entity.Client;
import com.fincomp.clients.entity.DueDiligenceRegime;
import com.fincomp.flags.entity.Flag;
import com.fincomp.flags.entity.FlagType;
import com.fincomp.flags.repository.FlagRepository;
import com.fincomp.monitoring.entity.Transaction;
import com.fincomp.monitoring.repository.TransactionRepository;
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

    @Value("${fincomp.compliance.simplified-regime.monthly-limit:30000.00}")
    private BigDecimal monthlyLimit;

    @Value("${fincomp.compliance.simplified-regime.annual-limit:240000.00}")
    private BigDecimal annualLimit;

    public DueDiligenceRegimeValidator(TransactionRepository transactionRepository, FlagRepository flagRepository) {
        this.transactionRepository = transactionRepository;
        this.flagRepository = flagRepository;
    }

    @Transactional
    public Optional<Flag> validateClientRegimeCompliance(Client client, Transaction newTransaction) {
        if (client == null || client.getDueDiligenceRegime() != DueDiligenceRegime.SIMPLIFIED) {
            return Optional.empty();
        }

        LocalDate txDate = newTransaction != null ? newTransaction.getTransactionDate() : LocalDate.now();
        LocalDate startOfMonth = txDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = txDate.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal monthlyTotal = transactionRepository.sumAmountByClientAndDateBetween(client.getId(), startOfMonth, endOfMonth);

        if (monthlyTotal.compareTo(monthlyLimit) > 0) {
            String reason = String.format(
                    "Cliente em Regime Simplificado excedeu o limiar mensal de %.2f MT (total movimentado no mês: %.2f MT). " +
                    "Obrigatória reclassificação para Regime Normal e recolha de diligência completa (Artigo 137 e 141 do Aviso 10/GBM/2024).",
                    monthlyLimit, monthlyTotal
            );

            Flag flag = new Flag(client, newTransaction, FlagType.KYC_INCOMPLETE, reason, null);
            return Optional.of(flagRepository.save(flag));
        }

        return Optional.empty();
    }
}
