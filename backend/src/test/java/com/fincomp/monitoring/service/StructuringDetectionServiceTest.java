package com.fincomp.monitoring.service;

import com.fincomp.clients.entity.Client;
import com.fincomp.flags.entity.FlagStatus;
import com.fincomp.flags.entity.FlagType;
import com.fincomp.flags.repository.FlagRepository;
import com.fincomp.monitoring.entity.ThresholdRule;
import com.fincomp.monitoring.entity.ThresholdRuleType;
import com.fincomp.monitoring.entity.Transaction;
import com.fincomp.monitoring.entity.TransactionType;
import com.fincomp.monitoring.repository.ThresholdRuleRepository;
import com.fincomp.monitoring.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StructuringDetectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ThresholdRuleRepository ruleRepository;
    @Mock
    private FlagRepository flagRepository;

    @Test
    void ignoresNonCashTransactions() {
        StructuringDetectionService service = new StructuringDetectionService(
                transactionRepository, ruleRepository, flagRepository);
        Client client = new Client("Cliente", "BI", "123");
        Transaction transfer = new Transaction(client, new BigDecimal("1000"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.TRANSFER);

        assertThat(service.detectStructuring(transfer)).isEmpty();
        verifyNoInteractions(ruleRepository, transactionRepository, flagRepository);
    }

    @Test
    void requiresAllTransactionsToBeBelowStructuringThreshold() {
        StructuringDetectionService service = new StructuringDetectionService(
                transactionRepository, ruleRepository, flagRepository);
        Client client = new Client("Cliente", "BI", "123");
        ThresholdRule rule = new ThresholdRule("Structuring", ThresholdRuleType.STRUCTURING,
                new BigDecimal("100000"), 30, "Art. 82");
        Transaction current = new Transaction(client, new BigDecimal("50000"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        Transaction aboveThreshold = new Transaction(client, new BigDecimal("100000"), "MZN",
                LocalDate.of(2026, 9, 8), TransactionType.DEPOSIT);

        when(ruleRepository.findByTypeAndActiveTrue(ThresholdRuleType.STRUCTURING)).thenReturn(List.of(rule));
        when(transactionRepository.findByClientAndDateBetweenAndTypeIn(
                any(), any(), any(), any())).thenReturn(List.of(current, aboveThreshold, current));

        assertThat(service.detectStructuring(current)).isEmpty();
        verify(flagRepository, never()).save(any());
    }

    @Test
    void doesNotCreateDuplicateStructuringFlagForSameTransaction() {
        StructuringDetectionService service = new StructuringDetectionService(
                transactionRepository, ruleRepository, flagRepository);
        Client client = new Client("Cliente", "BI", "123");
        Transaction current = new Transaction(client, new BigDecimal("50000"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        current.setId(UUID.randomUUID());
        ThresholdRule rule = new ThresholdRule("Structuring", ThresholdRuleType.STRUCTURING,
                new BigDecimal("100000"), 30, "Art. 82");

        when(ruleRepository.findByTypeAndActiveTrue(ThresholdRuleType.STRUCTURING)).thenReturn(List.of(rule));
        when(transactionRepository.findByClientAndDateBetweenAndTypeIn(
                any(), any(), any(), any())).thenReturn(List.of(current, current, current));
        when(flagRepository.existsByTransactionIdAndType(current.getId(), FlagType.STRUCTURING_SUSPECTED))
                .thenReturn(true);

        assertThat(service.detectStructuring(current)).isEmpty();
        verify(flagRepository, never()).save(any());
    }
}
