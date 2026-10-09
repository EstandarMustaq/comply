package com.comply.monitoring.service;

import com.comply.flags.entity.Flag;
import com.comply.flags.entity.FlagType;
import com.comply.flags.repository.FlagRepository;
import com.comply.monitoring.entity.ThresholdRule;
import com.comply.monitoring.entity.ThresholdRuleType;
import com.comply.monitoring.entity.Transaction;
import com.comply.monitoring.entity.TransactionType;
import com.comply.monitoring.repository.ThresholdRuleRepository;
import com.comply.monitoring.repository.TransactionRepository;
import com.comply.reporting.entity.CurrencyTransactionReport;
import com.comply.reporting.repository.CurrencyTransactionReportRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThresholdScreeningService {

  private static final List<TransactionType> CASH_TRANSACTION_TYPES =
      List.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL);

  private final ThresholdRuleRepository ruleRepository;
  private final TransactionRepository transactionRepository;
  private final FlagRepository flagRepository;
  private final CurrencyTransactionReportRepository ctrRepository;

  public ThresholdScreeningService(
      ThresholdRuleRepository ruleRepository,
      TransactionRepository transactionRepository,
      FlagRepository flagRepository,
      CurrencyTransactionReportRepository ctrRepository) {
    this.ruleRepository = ruleRepository;
    this.transactionRepository = transactionRepository;
    this.flagRepository = flagRepository;
    this.ctrRepository = ctrRepository;
  }

  @Transactional
  public List<Flag> evaluateTransaction(Transaction tx) {
    List<Flag> generatedFlags = new ArrayList<>();
    if (tx == null || tx.getClient() == null) return generatedFlags;

    if (tx.getClient().getOrganization() == null
        || tx.getClient().getOrganization().getId() == null) {
      throw new IllegalStateException(
          "The customer's Organization is required to screen this transaction.");
    }
    var organizationId = tx.getClient().getOrganization().getId();
    List<ThresholdRule> activeRules = ruleRepository.findByActiveTrue();
    if (!CASH_TRANSACTION_TYPES.contains(tx.getType())) return generatedFlags;

    for (ThresholdRule rule : activeRules) {
      if (rule.getType() == ThresholdRuleType.SINGLE_TRANSACTION_VALUE) {
        if (rule.getValueThreshold() != null
            && tx.getAmount().compareTo(rule.getValueThreshold()) >= 0) {
          String reason =
              String.format(
                  "Cash transaction meets or exceeds the configured limit of %.2f MT (amount: %.2f MT).",
                  rule.getValueThreshold(), tx.getAmount());
          if (flagRepository.countByTransactionIdAndOrganizationIdAndType(
                  tx.getId(), organizationId, FlagType.THRESHOLD_VALUE)
              == 0) {
            Flag flag =
                flagRepository.save(
                    new Flag(tx.getClient(), tx, FlagType.THRESHOLD_VALUE, reason, null));
            generatedFlags.add(flag);
          }

          // Create a draft cash transaction report.
          CurrencyTransactionReport ctr =
              new CurrencyTransactionReport(
                  tx.getClient(),
                  tx.getTransactionDate(),
                  tx.getTransactionDate(),
                  tx.getAmount(),
                  rule);
          if (rule.getId() != null
              && ctrRepository
                      .countByClientIdAndOrganizationIdAndAggregationPeriodStartAndAggregationPeriodEndAndThresholdRuleId(
                          tx.getClient().getId(),
                          organizationId,
                          tx.getTransactionDate(),
                          tx.getTransactionDate(),
                          rule.getId())
                  == 0) {
            ctrRepository.save(ctr);
          }
        }
      } else if (rule.getType() == ThresholdRuleType.MONTHLY_AGGREGATE_VALUE) {
        int windowDays =
            rule.getAggregationWindowDays() != null ? rule.getAggregationWindowDays() : 30;
        LocalDate startDate = tx.getTransactionDate().minusDays(Math.max(windowDays - 1, 0));
        LocalDate endDate = tx.getTransactionDate();

        BigDecimal aggregate =
            transactionRepository.sumAmountByClientAndOrganizationAndDateBetweenAndTypeIn(
                tx.getClient().getId(), organizationId, startDate, endDate, CASH_TRANSACTION_TYPES);
        if (rule.getValueThreshold() != null
            && aggregate.compareTo(rule.getValueThreshold()) >= 0) {
          String reason =
              String.format(
                  "The customer's total transactions over %d days exceed the configured limit of %.2f MT (total: %.2f MT).",
                  windowDays, rule.getValueThreshold(), aggregate);
          if (flagRepository.countByTransactionIdAndOrganizationIdAndType(
                  tx.getId(), organizationId, FlagType.THRESHOLD_VALUE)
              == 0) {
            Flag flag =
                flagRepository.save(
                    new Flag(tx.getClient(), tx, FlagType.THRESHOLD_VALUE, reason, null));
            generatedFlags.add(flag);
          }

          CurrencyTransactionReport ctr =
              new CurrencyTransactionReport(tx.getClient(), startDate, endDate, aggregate, rule);
          if (rule.getId() != null
              && ctrRepository
                      .countByClientIdAndOrganizationIdAndAggregationPeriodStartAndAggregationPeriodEndAndThresholdRuleId(
                          tx.getClient().getId(), organizationId, startDate, endDate, rule.getId())
                  == 0) {
            ctrRepository.save(ctr);
          }
        }
      }
    }

    return generatedFlags;
  }
}
