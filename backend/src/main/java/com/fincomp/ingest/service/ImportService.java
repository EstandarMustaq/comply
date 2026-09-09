package com.fincomp.ingest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.clients.entity.Client;
import com.fincomp.clients.repository.ClientRepository;
import com.fincomp.compliance.service.DueDiligenceRegimeValidator;
import com.fincomp.ingest.entity.ImportBatch;
import com.fincomp.ingest.entity.ImportFormat;
import com.fincomp.ingest.repository.ImportBatchRepository;
import com.fincomp.insights.service.InsightsService;
import com.fincomp.monitoring.entity.Transaction;
import com.fincomp.monitoring.entity.TransactionType;
import com.fincomp.monitoring.repository.TransactionRepository;
import com.fincomp.monitoring.service.StructuringDetectionService;
import com.fincomp.monitoring.service.ThresholdScreeningService;
import com.fincomp.sanctions.service.SanctionsScreeningService;
import com.fincomp.users.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImportService {

    private final ImportBatchRepository importBatchRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final CsvParser csvParser;
    private final XlsxParser xlsxParser;
    private final ColumnMappingService columnMappingService;
    private final SanctionsScreeningService sanctionsScreeningService;
    private final ThresholdScreeningService thresholdScreeningService;
    private final StructuringDetectionService structuringDetectionService;
    private final DueDiligenceRegimeValidator dueDiligenceRegimeValidator;
    private final InsightsService insightsService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // Cache temporário em memória para arquivos em fase de mapeamento antes da confirmação
    private final Map<UUID, byte[]> pendingFiles = new ConcurrentHashMap<>();

    public ImportService(
            ImportBatchRepository importBatchRepository,
            ClientRepository clientRepository,
            TransactionRepository transactionRepository,
            CsvParser csvParser,
            XlsxParser xlsxParser,
            ColumnMappingService columnMappingService,
            SanctionsScreeningService sanctionsScreeningService,
            ThresholdScreeningService thresholdScreeningService,
            StructuringDetectionService structuringDetectionService,
            DueDiligenceRegimeValidator dueDiligenceRegimeValidator,
            InsightsService insightsService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.importBatchRepository = importBatchRepository;
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
        this.csvParser = csvParser;
        this.xlsxParser = xlsxParser;
        this.columnMappingService = columnMappingService;
        this.sanctionsScreeningService = sanctionsScreeningService;
        this.thresholdScreeningService = thresholdScreeningService;
        this.structuringDetectionService = structuringDetectionService;
        this.dueDiligenceRegimeValidator = dueDiligenceRegimeValidator;
        this.insightsService = insightsService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public record UploadPreviewResponse(
            UUID batchId,
            String fileName,
            ImportFormat format,
            List<String> headers,
            List<List<String>> previewRows,
            Map<String, String> suggestedMapping
    ) {}

    public UploadPreviewResponse createUploadBatch(MultipartFile file, User user) throws IOException {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "extrato";
        ImportFormat format = originalFilename.toLowerCase().endsWith(".xlsx") ? ImportFormat.XLSX : ImportFormat.CSV;

        byte[] bytes = file.getBytes();
        List<String> headers;
        List<List<String>> rows;

        if (format == ImportFormat.XLSX) {
            XlsxParser.XlsxParsedData data = xlsxParser.parse(bytes);
            headers = data.headers();
            rows = data.rows();
        } else {
            CsvParser.CsvParsedData data = csvParser.parse(bytes);
            headers = data.headers();
            rows = data.rows();
        }

        ImportBatch batch = new ImportBatch(originalFilename, format, user);
        batch.setTotalRows(rows.size());
        ImportBatch savedBatch = importBatchRepository.save(batch);

        pendingFiles.put(savedBatch.getId(), bytes);

        Map<String, String> suggestedMapping = columnMappingService.suggestMapping(headers);
        List<List<String>> preview = rows.subList(0, Math.min(rows.size(), 5));

        return new UploadPreviewResponse(savedBatch.getId(), originalFilename, format, headers, preview, suggestedMapping);
    }

    @Transactional
    public Map<String, Object> confirmAndProcess(UUID batchId, Map<String, String> columnMapping, User user) throws Exception {
        ImportBatch batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote de importação não encontrado: " + batchId));

        byte[] bytes = pendingFiles.get(batchId);
        if (bytes == null) {
            throw new IllegalStateException("Arquivo do lote não encontrado ou já processado");
        }

        List<String> headers;
        List<List<String>> rows;
        if (batch.getFormat() == ImportFormat.XLSX) {
            XlsxParser.XlsxParsedData data = xlsxParser.parse(bytes);
            headers = data.headers();
            rows = data.rows();
        } else {
            CsvParser.CsvParsedData data = csvParser.parse(bytes);
            headers = data.headers();
            rows = data.rows();
        }

        batch.setColumnMapping(objectMapper.writeValueAsString(columnMapping));

        // Mapeia índices das colunas selecionadas
        int docIdx = getIndex(headers, columnMapping.get("clientDocument"));
        int nameIdx = getIndex(headers, columnMapping.get("clientName"));
        int dateIdx = getIndex(headers, columnMapping.get("transactionDate"));
        int amountIdx = getIndex(headers, columnMapping.get("amount"));
        int typeIdx = getIndex(headers, columnMapping.get("transactionType"));
        int accountIdx = getIndex(headers, columnMapping.get("accountNumber"));

        int successCount = 0;
        int errorCount = 0;
        List<Map<String, Object>> errorDetails = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                String docNumber = docIdx >= 0 && docIdx < row.size() ? row.get(docIdx).trim() : null;
                String clientName = nameIdx >= 0 && nameIdx < row.size() ? row.get(nameIdx).trim() : "Cliente Desconhecido";
                String dateStr = dateIdx >= 0 && dateIdx < row.size() ? row.get(dateIdx).trim() : null;
                String amountStr = amountIdx >= 0 && amountIdx < row.size() ? row.get(amountIdx).trim() : null;
                String typeStr = typeIdx >= 0 && typeIdx < row.size() ? row.get(typeIdx).trim() : null;
                String accountStr = accountIdx >= 0 && accountIdx < row.size() ? row.get(accountIdx).trim() : null;

                if (docNumber == null || docNumber.isBlank() || amountStr == null || amountStr.isBlank()) {
                    throw new IllegalArgumentException("Documento ou montante ausente na linha");
                }

                // Validate the complete row before creating or mutating any client data.
                LocalDate txDate = parseDate(dateStr);
                BigDecimal amount = parseAmount(amountStr);
                TransactionType txType = parseTransactionType(typeStr);

                // 1. Obtém ou cadastra cliente
                Client client = clientRepository.findByIdDocumentNumber(docNumber).orElseGet(() -> {
                    String docType = docNumber.length() == 9 ? "NUIT" : "BI";
                    Client newClient = new Client(clientName, docType, docNumber);
                    if (accountStr != null && !accountStr.isBlank()) {
                        newClient.setNuib(accountStr);
                    }
                    return clientRepository.save(newClient);
                });

                if (client.isSanctionsFrozen()) {
                    throw new IllegalStateException(
                            "Operação bloqueada: cliente congelado por correspondência em lista de sanções"
                    );
                }

                // 2. Criação da transação com retenção legal de 10 anos (Artigo 88)
                Transaction tx = new Transaction(client, amount, "MZN", txDate, txType);
                tx.setImportBatch(batch);
                if (accountStr != null) {
                    tx.setCounterpartyDetails("{\"account\": \"" + accountStr + "\"}");
                }
                Transaction savedTx = transactionRepository.save(tx);

                // 3. Execução síncrona dos motores regulatórios
                sanctionsScreeningService.screenClientAgainstSanctions(client, savedTx);
                thresholdScreeningService.evaluateTransaction(savedTx);
                structuringDetectionService.detectStructuring(savedTx);
                dueDiligenceRegimeValidator.validateClientRegimeCompliance(client, savedTx);

                successCount++;
            } catch (Exception e) {
                errorCount++;
                errorDetails.add(Map.of("row", i + 1, "error", e.getMessage()));
            }
        }

        batch.setTotalRows(rows.size());
        batch.setErrorRows(errorCount);
        importBatchRepository.save(batch);

        pendingFiles.remove(batchId);
        insightsService.evictInsightsCache();

        auditService.record(user, AuditAction.UPLOAD, "import_batches", batch.getId(),
                "{\"fileName\": \"" + batch.getFileName() + "\", \"successRows\": " + successCount + ", \"errorRows\": " + errorCount + "}");

        return Map.of(
                "batchId", batch.getId(),
                "status", "COMPLETED",
                "totalProcessed", successCount,
                "errorsCount", errorCount,
                "errors", errorDetails
        );
    }

    private int getIndex(List<String> headers, String targetHeader) {
        if (targetHeader == null) return -1;
        return headers.indexOf(targetHeader);
    }

    LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Data da transação ausente");
        }
        String normalizedDate = dateStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT)
        );
        for (DateTimeFormatter dtf : formatters) {
            try {
                LocalDate parsed = LocalDate.parse(normalizedDate, dtf);
                if (parsed.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Data da transação não pode estar no futuro: " + dateStr);
                }
                return parsed;
            } catch (java.time.format.DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        throw new IllegalArgumentException("Data da transação inválida: " + dateStr);
    }

    BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Montante da transação ausente");
        }
        String clean = amountStr.replaceAll("[^0-9,.-]", "").trim();
        if (clean.isBlank() || clean.equals("-") || clean.equals(".") || clean.equals(",")) {
            throw new IllegalArgumentException("Montante da transação inválido: " + amountStr);
        }
        if (clean.contains(",") && clean.contains(".")) {
            if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
                clean = clean.replace(".", "").replace(",", ".");
            } else {
                clean = clean.replace(",", "");
            }
        } else if (clean.contains(",")) {
            clean = clean.replace(",", ".");
        }
        try {
            BigDecimal amount = new BigDecimal(clean);
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Montante da transação deve ser positivo");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Montante da transação inválido: " + amountStr, exception);
        }
    }

    TransactionType parseTransactionType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            throw new IllegalArgumentException("Tipo de transação ausente");
        }
        String upper = typeStr.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("LEV") || upper.contains("SAQ") || upper.contains("WITHDRAW")) return TransactionType.WITHDRAWAL;
        if (upper.contains("TRANS")) return TransactionType.TRANSFER;
        if (upper.contains("CRED") || upper.contains("EMP")) return TransactionType.CREDIT;
        if (upper.contains("DEP") || upper.contains("DEPÓSITO") || upper.contains("DEPOSITO")) {
            return TransactionType.DEPOSIT;
        }
        throw new IllegalArgumentException("Tipo de transação inválido: " + typeStr);
    }
}
