package com.fincomp.reporting.service;

import com.fincomp.monitoring.repository.TransactionRepository;
import com.fincomp.reporting.entity.CurrencyTransactionReport;
import com.fincomp.reporting.entity.SuspiciousActivityReport;
import com.fincomp.reporting.repository.CurrencyTransactionReportRepository;
import com.fincomp.reporting.repository.SuspiciousActivityReportRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class RegulatoryExportService {

    private final SuspiciousActivityReportRepository strRepository;
    private final CurrencyTransactionReportRepository ctrRepository;
    private final TransactionRepository transactionRepository;

    public RegulatoryExportService(
            SuspiciousActivityReportRepository strRepository,
            CurrencyTransactionReportRepository ctrRepository,
            TransactionRepository transactionRepository
    ) {
        this.strRepository = strRepository;
        this.ctrRepository = ctrRepository;
        this.transactionRepository = transactionRepository;
    }

    public byte[] exportStrPdf(SuspiciousActivityReport report) {
        String text = String.join("\n",
                "RELATORIO DE OPERACAO SUSPEITA - GIFiM",
                "Referencia: " + report.getId(),
                "Instituicao: " + report.getReportingInstitution(),
                "Titular: " + report.getAccountHolder(),
                "Conta: " + value(report.getAccountNumber()),
                "Executor: " + value(report.getTransactionExecutor()),
                "Valor: " + value(report.getMonetaryValue()),
                "Suspeita: " + report.getSuspicionDescription(),
                "Relacao de negocio: " + report.getBusinessRelationshipDescription(),
                "Funcionario envolvido: " + report.isInternalEmployeeInvolved(),
                "Declaracao: " + value(report.getVoluntaryStatement()),
                "OCOS: " + report.getOcosName(),
                "Registo GIFiM: " + value(report.getGifimRegistrationNumber()),
                "Submetido em: " + value(report.getSubmittedAt()),
                "Retencao ate: " + value(report.getRetentionUntil())
        );
        return createPdf(text);
    }

    public byte[] exportAnnualExcel(int year) throws IOException {
        if (year < 2000 || year > LocalDate.now().getYear() + 1) {
            throw new IllegalArgumentException("Ano do relatório inválido");
        }
        LocalDate start = LocalDate.of(year, Month.JANUARY, 1);
        LocalDate end = LocalDate.of(year, Month.DECEMBER, 31);
        Instant startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<SuspiciousActivityReport> strs = strRepository.findBySubmittedAtBetween(startInstant, endInstant);
        List<CurrencyTransactionReport> ctrs = ctrRepository.findByAggregationPeriodStartBetween(start, end);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet summary = workbook.createSheet("Resumo BM");
            row(summary, 0, "RELATORIO ANUAL BM", year);
            row(summary, 1, "Total transacoes", transactionRepository.countByTransactionDateBetween(start, end));
            row(summary, 2, "Volume total MZN", transactionRepository.sumAmountByTransactionDateBetween(start, end));
            row(summary, 3, "STR submetidas", strs.size());
            row(summary, 4, "CTR gerados", ctrs.size());

            Sheet strSheet = workbook.createSheet("STR");
            row(strSheet, 0, "ID", "Instituicao", "Titular", "Valor", "GIFiM", "Submetido em");
            int index = 1;
            for (SuspiciousActivityReport str : strs) {
                row(strSheet, index++, str.getId(), str.getReportingInstitution(), str.getAccountHolder(),
                        str.getMonetaryValue(), str.getGifimRegistrationNumber(), str.getSubmittedAt());
            }

            Sheet ctrSheet = workbook.createSheet("CTR");
            row(ctrSheet, 0, "ID", "Inicio", "Fim", "Valor", "Moeda", "Submetido em");
            index = 1;
            for (CurrencyTransactionReport ctr : ctrs) {
                row(ctrSheet, index++, ctr.getId(), ctr.getAggregationPeriodStart(),
                        ctr.getAggregationPeriodEnd(), ctr.getAggregateValue(), ctr.getCurrency(), ctr.getSubmittedAt());
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void row(Sheet sheet, int index, Object... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(value(values[i]));
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private byte[] createPdf(String text) {
        String escaped = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\n", ") Tj 0 -14 Td (");
        String stream = "BT /F1 10 Tf 50 780 Td (" + escaped + ") Tj ET";
        byte[] streamBytes = stream.getBytes(StandardCharsets.US_ASCII);
        String header = "%PDF-1.4\n";
        String[] objects = {
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n",
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n",
                "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n",
                "5 0 obj << /Length " + streamBytes.length + " >> stream\n" + stream + "\nendstream endobj\n"
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write(header.getBytes(StandardCharsets.US_ASCII));
            int[] offsets = new int[objects.length];
            for (int i = 0; i < objects.length; i++) {
                offsets[i] = output.size();
                output.write(objects[i].getBytes(StandardCharsets.US_ASCII));
            }
            int xref = output.size();
            output.write(("xref\n0 6\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
            for (int offset : offsets) {
                output.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.US_ASCII));
            }
            output.write(("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n")
                    .getBytes(StandardCharsets.US_ASCII));
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao gerar PDF regulatório", exception);
        }
    }
}
