package com.fincomp.compliance.service;

import com.fincomp.clients.entity.Client;
import com.fincomp.clients.entity.DueDiligenceRegime;
import com.fincomp.clients.repository.ClientRepository;
import com.fincomp.flags.entity.Flag;
import com.fincomp.flags.entity.FlagStatus;
import com.fincomp.flags.entity.FlagType;
import com.fincomp.flags.repository.FlagRepository;
import com.fincomp.monitoring.entity.Transaction;
import com.fincomp.monitoring.entity.TransactionType;
import com.fincomp.monitoring.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueDiligenceRegimeValidatorTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FlagRepository flagRepository;
    @Mock
    private ClientRepository clientRepository;

    private DueDiligenceRegimeValidator validator;
    private Client client;

    @BeforeEach
    void setUp() {
        validator = new DueDiligenceRegimeValidator(transactionRepository, flagRepository, clientRepository);
        ReflectionTestUtils.setField(validator, "monthlyLimit", new BigDecimal("30000.00"));
        ReflectionTestUtils.setField(validator, "annualLimit", new BigDecimal("240000.00"));

        client = new Client("Cliente", "BI", "123");
        client.setId(UUID.randomUUID());
        client.setDueDiligenceRegime(DueDiligenceRegime.SIMPLIFIED);
    }

    @Test
    void promotesClientWhenMonthlyLimitIsExceededByNewTransaction() {
        Transaction transaction = new Transaction(client, new BigDecimal("35000.00"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        when(transactionRepository.sumAmountByClientAndDateBetween(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(flagRepository.existsByClientIdAndTypeAndStatus(any(), eq(FlagType.KYC_INCOMPLETE), eq(FlagStatus.PENDING)))
                .thenReturn(false);
        when(flagRepository.save(any(Flag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Flag> result = validator.validateClientRegimeCompliance(client, transaction);

        assertThat(result).isPresent();
        assertThat(client.getDueDiligenceRegime()).isEqualTo(DueDiligenceRegime.NORMAL);
        assertThat(client.isKycComplete()).isFalse();
        verify(clientRepository).save(client);
        assertThat(result.orElseThrow().getReason()).contains("limiar mensal");
    }

    @Test
    void promotesClientWhenAnnualLimitIsExceeded() {
        Transaction transaction = new Transaction(client, new BigDecimal("1000.00"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        when(transactionRepository.sumAmountByClientAndDateBetween(
                eq(client.getId()), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(new BigDecimal("240000.00"));
        when(transactionRepository.sumAmountByClientAndDateBetween(
                eq(client.getId()), eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 30))))
                .thenReturn(BigDecimal.ZERO);
        when(flagRepository.existsByClientIdAndTypeAndStatus(any(), eq(FlagType.KYC_INCOMPLETE), eq(FlagStatus.PENDING)))
                .thenReturn(false);
        when(flagRepository.save(any(Flag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Flag> result = validator.validateClientRegimeCompliance(client, transaction);

        assertThat(result).isPresent();
        assertThat(client.getDueDiligenceRegime()).isEqualTo(DueDiligenceRegime.NORMAL);
        assertThat(result.orElseThrow().getReason()).contains("limiar anual");
    }

    @Test
    void doesNotFlagClientBelowBothLimits() {
        Transaction transaction = new Transaction(client, new BigDecimal("1000.00"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        when(transactionRepository.sumAmountByClientAndDateBetween(any(), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));

        assertThat(validator.validateClientRegimeCompliance(client, transaction)).isEmpty();
        verifyNoInteractions(clientRepository, flagRepository);
    }
}
