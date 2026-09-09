package com.fincomp.monitoring.service;

import com.fincomp.flags.entity.Flag;
import com.fincomp.flags.entity.FlagType;
import com.fincomp.flags.repository.FlagRepository;
import com.fincomp.monitoring.entity.ThresholdRule;
import com.fincomp.monitoring.entity.ThresholdRuleType;
import com.fincomp.monitoring.entity.Transaction;
import com.fincomp.monitoring.repository.ThresholdRuleRepository;
import com.fincomp.monitoring.repository.TransactionRepository;
import com.fincomp.reporting.entity.CurrencyTransactionReport;
import com.fincomp.reporting.repository.CurrencyTransactionReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ThresholdScreeningService {

    private final ThresholdRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final FlagRepository flagRepository;
    private final CurrencyTransactionReportRepository ctrRepository;

    public ThresholdScreeningService(
            ThresholdRuleRepository ruleRepository,
            TransactionRepository transactionRepository,
            FlagRepository flagRepository,
            CurrencyTransactionReportRepository ctrRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.flagRepository = flagRepository;
        this.ctrRepository = ctrRepository;
    }

    @Transactional
    public List<Flag> evaluateTransaction(Transaction tx) {
        List<Flag> generatedFlags = new ArrayList<>();
        if (tx == null || tx.getClient() == null) return generatedFlags;

        List<ThresholdRule> activeRules = ruleRepository.findByActiveTrue();

        for (ThresholdRule rule : activeRules) {
            if (rule.getType() == ThresholdRuleType.SINGLE_TRANSACTION_VALUE) {
                if (rule.getValueThreshold() != null && tx.getAmount().compareTo(rule.getValueThreshold()) >= 0) {
                    String reason = String.format(
                            "Operação relevante em numerário que atinge ou ultrapassa o limiar legal de %.2f MT (valor: %.2f MT). Base Legal: %s.",
                            rule.getValueThreshold(), tx.getAmount(), rule.getLegalBasis()
                    );
                    Flag flag = flagRepository.save(new Flag(tx.getClient(), tx, FlagType.THRESHOLD_VALUE, reason, null));
                    generatedFlags.add(flag);

                    // Cria minuta de CTR (Comunicação de Operações em Espécie)
                    CurrencyTransactionReport ctr = new CurrencyTransactionReport(
                            tx.getClient(), tx.getTransactionDate(), tx.getTransactionDate(), tx.getAmount(), rule
                    );
                    ctrRepository.save(ctr);
                }
            } else if (rule.getType() == ThresholdRuleType.MONTHLY_AGGREGATE_VALUE) {
                int windowDays = rule.getAggregationWindowDays() != null ? rule.getAggregationWindowDays() : 30;
                LocalDate startDate = tx.getTransactionDate().minusDays(windowDays);
                LocalDate endDate = tx.getTransactionDate();

                BigDecimal aggregate = transactionRepository.sumAmountByClientAndDateBetween(tx.getClient().getId(), startDate, endDate);
                if (rule.getValueThreshold() != null && aggregate.compareTo(rule.getValueThreshold()) >= 0) {
                    String reason = String.format(
                            "Valor agregado de transacções do mesmo cliente na janela de %d dias ultrapassa o limiar legal de %.2f MT (total acumulado: %.2f MT). Base Legal: %s.",
                            windowDays, rule.getValueThreshold(), aggregate, rule.getLegalBasis()
                    );
                    Flag flag = flagRepository.save(new Flag(tx.getClient(), tx, FlagType.THRESHOLD_VALUE, reason, null));
                    generatedFlags.add(flag);

                    CurrencyTransactionReport ctr = new CurrencyTransactionReport(
                            tx.getClient(), startDate, endDate, aggregate, rule
                    );
                    ctrRepository.save(ctr);
                }
            }
        }

        return generatedFlags;
    }
}
