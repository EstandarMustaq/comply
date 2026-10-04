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
import com.complyz.reporting.repository.CurrencyTransactionReportRepository;
import com.complyz.tenancy.entity.Organization;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThresholdScreeningServiceTenantScopeTest {

    @Mock ThresholdRuleRepository ruleRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock FlagRepository flagRepository;
    @Mock CurrencyTransactionReportRepository reportRepository;

    @Test
    void generatedRecordsAndDuplicateChecksUseTheClientOrganization() {
        UUID organizationId = UUID.randomUUID();
        Client client = client(organizationId);
        Transaction transaction = new Transaction(client, new BigDecimal("15000"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        transaction.setId(UUID.randomUUID());
        ThresholdRule rule = new ThresholdRule("Single transaction", ThresholdRuleType.SINGLE_TRANSACTION_VALUE,
                new BigDecimal("10000"), 30);
        rule.setId(UUID.randomUUID());
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(flagRepository.countByTransactionIdAndOrganizationIdAndType(
                transaction.getId(), organizationId, FlagType.THRESHOLD_VALUE)).thenReturn(0L);
        when(reportRepository.countByClientIdAndOrganizationIdAndAggregationPeriodStartAndAggregationPeriodEndAndThresholdRuleId(
                client.getId(), organizationId, transaction.getTransactionDate(), transaction.getTransactionDate(),
                rule.getId())).thenReturn(0L);
        when(flagRepository.save(org.mockito.ArgumentMatchers.any(Flag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Flag> flags = service().evaluateTransaction(transaction);

        assertThat(flags).hasSize(1);
        verify(flagRepository).countByTransactionIdAndOrganizationIdAndType(
                transaction.getId(), organizationId, FlagType.THRESHOLD_VALUE);
        verify(reportRepository).countByClientIdAndOrganizationIdAndAggregationPeriodStartAndAggregationPeriodEndAndThresholdRuleId(
                client.getId(), organizationId, transaction.getTransactionDate(), transaction.getTransactionDate(),
                rule.getId());
    }

    @Test
    void transactionWithoutOrganizationIsRejectedBeforeLoadingGlobalRules() {
        Client client = new Client("Cliente", "BI", "123");
        Transaction transaction = new Transaction(client, new BigDecimal("15000"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);

        assertThatThrownBy(() -> service().evaluateTransaction(transaction))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(ruleRepository, transactionRepository, flagRepository, reportRepository);
    }

    private ThresholdScreeningService service() {
        return new ThresholdScreeningService(ruleRepository, transactionRepository, flagRepository, reportRepository);
    }

    private static Client client(UUID organizationId) {
        Client client = new Client("Cliente", "BI", "123");
        client.setId(UUID.randomUUID());
        Organization organization = new Organization("test-org", "Test Organization");
        organization.setId(organizationId);
        client.setOrganization(organization);
        return client;
    }
}
