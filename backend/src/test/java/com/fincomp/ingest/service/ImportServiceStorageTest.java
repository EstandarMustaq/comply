package com.fincomp.ingest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincomp.audit.service.AuditService;
import com.fincomp.clients.repository.ClientRepository;
import com.fincomp.compliance.service.DueDiligenceRegimeValidator;
import com.fincomp.ingest.entity.ImportBatch;
import com.fincomp.ingest.entity.ImportFormat;
import com.fincomp.ingest.repository.ImportBatchRepository;
import com.fincomp.insights.service.InsightsService;
import com.fincomp.monitoring.repository.TransactionRepository;
import com.fincomp.monitoring.service.StructuringDetectionService;
import com.fincomp.monitoring.service.ThresholdScreeningService;
import com.fincomp.sanctions.service.SanctionsScreeningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ImportServiceStorageTest {

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

    @Test
    void cleanupRemovesExpiredSourceFilesInsideConfiguredStorage() throws Exception {
        ImportService service = new ImportService(
                importBatchRepository, clientRepository, transactionRepository,
                csvParser, xlsxParser, columnMappingService, sanctionsScreeningService,
                thresholdScreeningService, structuringDetectionService,
                dueDiligenceRegimeValidator, insightsService, auditService, objectMapper
        );
        Path storage = Files.createTempDirectory("fincomp-import-test");
        Path source = Files.createFile(storage.resolve("batch.csv"));
        ImportBatch batch = new ImportBatch("batch.csv", ImportFormat.CSV, null);
        batch.setSourceFilePath(source.toString());
        ReflectionTestUtils.setField(service, "storagePath", storage.toString());
        ReflectionTestUtils.setField(service, "pendingTtlHours", 1L);
        when(importBatchRepository.findByImportedAtBefore(any(Instant.class))).thenReturn(List.of(batch));

        service.cleanupExpiredImportFiles();

        assertThat(Files.exists(source)).isFalse();
        assertThat(batch.getSourceFilePath()).isNull();
        verify(importBatchRepository).save(batch);
    }
}
