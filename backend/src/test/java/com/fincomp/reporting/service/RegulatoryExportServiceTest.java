package com.fincomp.reporting.service;

import com.fincomp.monitoring.repository.TransactionRepository;
import com.fincomp.reporting.entity.SuspiciousActivityReport;
import com.fincomp.reporting.repository.CurrencyTransactionReportRepository;
import com.fincomp.reporting.repository.SuspiciousActivityReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegulatoryExportServiceTest {

    @Mock SuspiciousActivityReportRepository strRepository;
    @Mock CurrencyTransactionReportRepository ctrRepository;
    @Mock TransactionRepository transactionRepository;

    @Test
    void createsPdfForSubmittedStr() {
        RegulatoryExportService service = new RegulatoryExportService(
                strRepository, ctrRepository, transactionRepository);
        SuspiciousActivityReport report = new SuspiciousActivityReport();
        report.setReportingInstitution("FinComp");
        report.setAccountHolder("Cliente");
        report.setSuspicionDescription("Operações atípicas");
        report.setBusinessRelationshipDescription("Conta de depósito");
        report.setOcosName("OCOS");
        report.markAsSubmitted("GIFIM-1");

        byte[] pdf = service.exportStrPdf(report);

        assertThat(pdf).startsWith("%PDF-1.4".getBytes());
        assertThat(new String(pdf)).contains("RELATORIO DE OPERACAO SUSPEITA");
    }

    @Test
    void createsAnnualExcelWithRegulatorySheets() throws Exception {
        RegulatoryExportService service = new RegulatoryExportService(
                strRepository, ctrRepository, transactionRepository);
        when(strRepository.findBySubmittedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(ctrRepository.findByAggregationPeriodStartBetween(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
                .thenReturn(List.of());
        when(transactionRepository.countByTransactionDateBetween(any(), any())).thenReturn(12L);
        when(transactionRepository.sumAmountByTransactionDateBetween(any(), any()))
                .thenReturn(new BigDecimal("1500.00"));

        byte[] xlsx = service.exportAnnualExcel(2025);

        assertThat(xlsx).startsWith(new byte[]{'P', 'K'});
        assertThat(xlsx).isNotEmpty();
    }
}
