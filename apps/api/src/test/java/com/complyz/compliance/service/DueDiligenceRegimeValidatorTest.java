package com.complyz.compliance.service;

import com.complyz.clients.entity.Client;
import com.complyz.clients.entity.DueDiligenceRegime;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.monitoring.entity.Transaction;
import com.complyz.monitoring.entity.TransactionType;
import com.complyz.monitoring.repository.TransactionRepository;
import com.complyz.tenancy.entity.Organization;
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
        Organization organization = new Organization("test-org", "Test Organization");
        organization.setId(UUID.randomUUID());
        client.setOrganization(organization);
        client.setDueDiligenceRegime(DueDiligenceRegime.SIMPLIFIED);
    }

    @Test
    void promotesClientWhenMonthlyLimitIsExceededByNewTransaction() {
        Transaction transaction = new Transaction(client, new BigDecimal("35000.00"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        when(transactionRepository.sumAmountByClientAndOrganizationAndDateBetween(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                eq(client.getId()), eq(client.getOrganization().getId()),
                eq(FlagType.KYC_INCOMPLETE), eq(FlagStatus.PENDING))).thenReturn(0L);
        when(flagRepository.save(any(Flag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Flag> result = validator.validateClientRegimeCompliance(client, transaction);

        assertThat(result).isPresent();
        assertThat(client.getDueDiligenceRegime()).isEqualTo(DueDiligenceRegime.NORMAL);
        assertThat(client.isKycComplete()).isFalse();
        verify(clientRepository).save(client);
        assertThat(result.orElseThrow().getReason()).contains("monthly limit");
    }

    @Test
    void promotesClientWhenAnnualLimitIsExceeded() {
        Transaction transaction = new Transaction(client, new BigDecimal("1000.00"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        when(transactionRepository.sumAmountByClientAndOrganizationAndDateBetween(
                eq(client.getId()), eq(client.getOrganization().getId()),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(new BigDecimal("240000.00"));
        when(transactionRepository.sumAmountByClientAndOrganizationAndDateBetween(
                eq(client.getId()), eq(client.getOrganization().getId()),
                eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 30))))
                .thenReturn(BigDecimal.ZERO);
        when(flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                eq(client.getId()), eq(client.getOrganization().getId()),
                eq(FlagType.KYC_INCOMPLETE), eq(FlagStatus.PENDING))).thenReturn(0L);
        when(flagRepository.save(any(Flag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Flag> result = validator.validateClientRegimeCompliance(client, transaction);

        assertThat(result).isPresent();
        assertThat(client.getDueDiligenceRegime()).isEqualTo(DueDiligenceRegime.NORMAL);
        assertThat(result.orElseThrow().getReason()).contains("annual limit");
    }

    @Test
    void doesNotFlagClientBelowBothLimits() {
        Transaction transaction = new Transaction(client, new BigDecimal("1000.00"), "MZN",
                LocalDate.of(2026, 9, 9), TransactionType.DEPOSIT);
        when(transactionRepository.sumAmountByClientAndOrganizationAndDateBetween(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));

        assertThat(validator.validateClientRegimeCompliance(client, transaction)).isEmpty();
        verifyNoInteractions(clientRepository, flagRepository);
    }
}
