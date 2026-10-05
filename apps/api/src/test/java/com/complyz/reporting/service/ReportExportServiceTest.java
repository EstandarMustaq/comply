package com.complyz.reporting.service;

import com.complyz.monitoring.repository.TransactionRepository;
import com.complyz.reporting.entity.SuspiciousActivityReport;
import com.complyz.reporting.repository.CurrencyTransactionReportRepository;
import com.complyz.reporting.repository.SuspiciousActivityReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock SuspiciousActivityReportRepository strRepository;
    @Mock CurrencyTransactionReportRepository ctrRepository;
    @Mock TransactionRepository transactionRepository;

    @Test
    void createsPdfForSubmittedStr() {
        ReportExportService service = new ReportExportService(
                strRepository, ctrRepository, transactionRepository);
        SuspiciousActivityReport report = new SuspiciousActivityReport();
        report.setReportingInstitution("Complyz");
        report.setAccountHolder("Cliente");
        report.setSuspicionDescription("Operações atípicas");
        report.setBusinessRelationshipDescription("Conta de depósito");
        report.setOcosName("OCOS");
        report.markAsSubmitted("GIFIM-1");

        byte[] pdf = service.exportStrPdf(report);

        assertThat(pdf).startsWith("%PDF-1.4".getBytes());
        String pdfContent = new String(pdf, java.nio.charset.StandardCharsets.US_ASCII);
        assertThat(pdfContent)
                .contains("Suspicious activity report")
                .contains("\\347")
                .contains("\\365")
                .contains("/Encoding /WinAnsiEncoding");
    }

    @Test
    void createsAnnualExcelWithReportSheets() throws Exception {
        ReportExportService service = new ReportExportService(
                strRepository, ctrRepository, transactionRepository);
        UUID organizationId = UUID.randomUUID();
        when(strRepository.findSubmittedBetweenForOrganization(
                any(Instant.class), any(Instant.class), eq(organizationId)))
                .thenReturn(List.of());
        when(ctrRepository.findByOrganizationAndAggregationPeriodStartBetween(
                organizationId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
                .thenReturn(List.of());
        when(transactionRepository.countByOrganizationAndTransactionDateBetween(
                eq(organizationId), any(), any())).thenReturn(12L);
        when(transactionRepository.sumAmountByOrganizationAndTransactionDateBetween(
                eq(organizationId), any(), any()))
                .thenReturn(new BigDecimal("1500.00"));

        byte[] xlsx = service.exportAnnualExcel(2025, organizationId);

        assertThat(xlsx).startsWith(new byte[]{'P', 'K'});
        assertThat(xlsx).isNotEmpty();
        verify(strRepository).findSubmittedBetweenForOrganization(
                any(Instant.class), any(Instant.class), eq(organizationId));
        verify(ctrRepository).findByOrganizationAndAggregationPeriodStartBetween(
                organizationId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        verify(transactionRepository).countByOrganizationAndTransactionDateBetween(
                organizationId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        verify(transactionRepository).sumAmountByOrganizationAndTransactionDateBetween(
                organizationId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }
}
