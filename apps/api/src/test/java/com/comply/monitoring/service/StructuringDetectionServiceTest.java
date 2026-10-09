package com.comply.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.comply.clients.entity.Client;
import com.comply.flags.entity.FlagType;
import com.comply.flags.repository.FlagRepository;
import com.comply.monitoring.entity.ThresholdRule;
import com.comply.monitoring.entity.ThresholdRuleType;
import com.comply.monitoring.entity.Transaction;
import com.comply.monitoring.entity.TransactionType;
import com.comply.monitoring.repository.ThresholdRuleRepository;
import com.comply.monitoring.repository.TransactionRepository;
import com.comply.tenancy.entity.Organization;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StructuringDetectionServiceTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private ThresholdRuleRepository ruleRepository;
  @Mock private FlagRepository flagRepository;

  @Test
  void ignoresNonCashTransactions() {
    StructuringDetectionService service =
        new StructuringDetectionService(transactionRepository, ruleRepository, flagRepository);
    Client client = new Client("Cliente", "BI", "123");
    Transaction transfer =
        new Transaction(
            client,
            new BigDecimal("1000"),
            "MZN",
            LocalDate.of(2026, 9, 9),
            TransactionType.TRANSFER);

    assertThat(service.detectStructuring(transfer)).isEmpty();
    verifyNoInteractions(ruleRepository, transactionRepository, flagRepository);
  }

  @Test
  void requiresAllTransactionsToBeBelowStructuringThreshold() {
    StructuringDetectionService service =
        new StructuringDetectionService(transactionRepository, ruleRepository, flagRepository);
    Client client = client();
    ThresholdRule rule =
        new ThresholdRule(
            "Structuring", ThresholdRuleType.STRUCTURING, new BigDecimal("100000"), 30);
    Transaction current =
        new Transaction(
            client,
            new BigDecimal("50000"),
            "MZN",
            LocalDate.of(2026, 9, 9),
            TransactionType.DEPOSIT);
    Transaction aboveThreshold =
        new Transaction(
            client,
            new BigDecimal("100000"),
            "MZN",
            LocalDate.of(2026, 9, 8),
            TransactionType.DEPOSIT);

    when(ruleRepository.findByTypeAndActiveTrue(ThresholdRuleType.STRUCTURING))
        .thenReturn(List.of(rule));
    when(transactionRepository.findByClientAndOrganizationAndDateBetweenAndTypeIn(
            any(), any(), any(), any(), any()))
        .thenReturn(List.of(current, aboveThreshold, current));

    assertThat(service.detectStructuring(current)).isEmpty();
    verify(flagRepository, never()).save(any());
  }

  @Test
  void doesNotCreateDuplicateStructuringFlagForSameTransaction() {
    StructuringDetectionService service =
        new StructuringDetectionService(transactionRepository, ruleRepository, flagRepository);
    Client client = client();
    Transaction current =
        new Transaction(
            client,
            new BigDecimal("50000"),
            "MZN",
            LocalDate.of(2026, 9, 9),
            TransactionType.DEPOSIT);
    current.setId(UUID.randomUUID());
    ThresholdRule rule =
        new ThresholdRule(
            "Structuring", ThresholdRuleType.STRUCTURING, new BigDecimal("100000"), 30);

    when(ruleRepository.findByTypeAndActiveTrue(ThresholdRuleType.STRUCTURING))
        .thenReturn(List.of(rule));
    when(transactionRepository.findByClientAndOrganizationAndDateBetweenAndTypeIn(
            any(), any(), any(), any(), any()))
        .thenReturn(List.of(current, current, current));
    when(flagRepository.countByTransactionIdAndOrganizationIdAndType(
            current.getId(), client.getOrganization().getId(), FlagType.STRUCTURING_SUSPECTED))
        .thenReturn(1L);

    assertThat(service.detectStructuring(current)).isEmpty();
    verify(flagRepository, never()).save(any());
  }

  private static Client client() {
    Client client = new Client("Cliente", "BI", "123");
    client.setId(UUID.randomUUID());
    Organization organization = new Organization("test-org", "Test Organization");
    organization.setId(UUID.randomUUID());
    client.setOrganization(organization);
    return client;
  }
}
