package com.complyz.ingest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.complyz.audit.service.AuditService;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.compliance.service.DueDiligenceRegimeValidator;
import com.complyz.ingest.repository.ImportBatchRepository;
import com.complyz.insights.service.InsightsService;
import com.complyz.monitoring.repository.TransactionRepository;
import com.complyz.monitoring.service.StructuringDetectionService;
import com.complyz.monitoring.service.ThresholdScreeningService;
import com.complyz.sanctions.service.SanctionsScreeningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ImportServiceValidationTest {

    @Mock ImportBatchRepository importBatchRepository;
    @Mock ClientRepository clientRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock CsvParser csvParser;
    @Mock XlsxParser xlsxParser;
    @Mock ColumnMappingService columnMappingService;
    @Mock SanctionsScreeningService sanctionsScreeningService;
    @Mock ThresholdScreeningService thresholdScreeningService;
    @Mock StructuringDetectionService structuringDetectionService;
    @Mock DueDiligenceRegimeValidator dueDiligenceRegimeValidator;
    @Mock InsightsService insightsService;
    @Mock AuditService auditService;
    @Mock ObjectMapper objectMapper;

    private ImportService service;

    @BeforeEach
    void setUp() {
        service = new ImportService(
                importBatchRepository, clientRepository, transactionRepository,
                csvParser, xlsxParser, columnMappingService, sanctionsScreeningService,
                thresholdScreeningService, structuringDetectionService,
                dueDiligenceRegimeValidator, insightsService, auditService, objectMapper
        );
    }

    @Test
    void parsesSupportedDateFormatsWithoutDefaultingInvalidDates() {
        assertThat(service.parseDate("2026-09-09")).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(service.parseDate("09/09/2026")).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(service.parseDate("09-09-2026")).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(service.parseDate("2026/09/09")).isEqualTo(LocalDate.of(2026, 9, 9));
    }

    @Test
    void rejectsMissingInvalidAndFutureDates() {
        assertThatThrownBy(() -> service.parseDate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ausente");
        assertThatThrownBy(() -> service.parseDate("31/02/2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválida");
        assertThatThrownBy(() -> service.parseDate("2099-01-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void parsesLocalizedPositiveAmounts() {
        assertThat(service.parseAmount("1.234,56")).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(service.parseAmount("1234.56 MT")).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    void rejectsMissingInvalidAndNonPositiveAmounts() {
        assertThatThrownBy(() -> service.parseAmount(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ausente");
        assertThatThrownBy(() -> service.parseAmount("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
        assertThatThrownBy(() -> service.parseAmount("-10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    @Test
    void rejectsUnknownTransactionTypesInsteadOfTreatingThemAsDeposits() {
        assertThat(service.parseTransactionType("DEPOSITO")).isEqualTo(com.complyz.monitoring.entity.TransactionType.DEPOSIT);
        assertThat(service.parseTransactionType("SAQUE")).isEqualTo(com.complyz.monitoring.entity.TransactionType.WITHDRAWAL);
        assertThatThrownBy(() -> service.parseTransactionType("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }
}
