package com.fincomp.reporting.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.reporting.entity.CurrencyTransactionReport;
import com.fincomp.reporting.entity.SuspiciousActivityReport;
import com.fincomp.reporting.repository.CurrencyTransactionReportRepository;
import com.fincomp.reporting.repository.SuspiciousActivityReportRepository;
import com.fincomp.reporting.service.RegulatoryExportService;
import com.fincomp.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Year;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SuspiciousActivityReportRepository strRepository;
    private final CurrencyTransactionReportRepository ctrRepository;
    private final AuditService auditService;
    private final RegulatoryExportService regulatoryExportService;

    public ReportController(
            SuspiciousActivityReportRepository strRepository,
            CurrencyTransactionReportRepository ctrRepository,
            AuditService auditService,
            RegulatoryExportService regulatoryExportService
    ) {
        this.strRepository = strRepository;
        this.ctrRepository = ctrRepository;
        this.auditService = auditService;
        this.regulatoryExportService = regulatoryExportService;
    }

    @GetMapping("/str")
    public ResponseEntity<List<SuspiciousActivityReport>> listStrs(@RequestParam(required = false) Boolean submitted) {
        if (Boolean.TRUE.equals(submitted)) {
            return ResponseEntity.ok(strRepository.findBySubmittedAtIsNotNull());
        }
        return ResponseEntity.ok(strRepository.findAll());
    }

    @GetMapping("/str/{id}")
    public ResponseEntity<SuspiciousActivityReport> getStr(@PathVariable UUID id) {
        return strRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record UpdateStrRequest(
            String accountNumber,
            String accountHolder,
            String transactionExecutor,
            BigDecimal monetaryValue,
            String suspicionDescription,
            String businessRelationshipDescription,
            boolean internalEmployeeInvolved,
            String voluntaryStatement
    ) {}

    @PutMapping("/str/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OCOS')")
    public ResponseEntity<?> updateStr(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStrRequest request,
            Authentication authentication
    ) {
        SuspiciousActivityReport str = strRepository.findById(id).orElse(null);
        if (str == null) return ResponseEntity.notFound().build();

        if (str.getSubmittedAt() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Relatório já submetido ao GIFiM não pode ser editado"));
        }

        if (request.accountNumber() != null) str.setAccountNumber(request.accountNumber());
        if (request.accountHolder() != null) str.setAccountHolder(request.accountHolder());
        if (request.transactionExecutor() != null) str.setTransactionExecutor(request.transactionExecutor());
        if (request.monetaryValue() != null) str.setMonetaryValue(request.monetaryValue());
        if (request.suspicionDescription() != null) str.setSuspicionDescription(request.suspicionDescription());
        if (request.businessRelationshipDescription() != null) str.setBusinessRelationshipDescription(request.businessRelationshipDescription());
        str.setInternalEmployeeInvolved(request.internalEmployeeInvolved());
        if (request.voluntaryStatement() != null) str.setVoluntaryStatement(request.voluntaryStatement());

        SuspiciousActivityReport saved = strRepository.save(str);
        return ResponseEntity.ok(saved);
    }

    public record SubmitStrRequest(@NotBlank String gifimRegistrationNumber) {}

    @PostMapping("/str/{id}/submit")
    @PreAuthorize("hasRole('OCOS')")
    public ResponseEntity<?> submitStr(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitStrRequest request,
            Authentication authentication
    ) {
        SuspiciousActivityReport str = strRepository.findById(id).orElse(null);
        if (str == null) return ResponseEntity.notFound().build();

        try {
            validateStrReadyForSubmission(str);
            str.markAsSubmitted(request.gifimRegistrationNumber());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
        }
        SuspiciousActivityReport saved = strRepository.save(str);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.STR_SUBMITTED, "suspicious_activity_reports", str.getId(),
                "{\"gifimRegistrationNumber\": \"" + request.gifimRegistrationNumber() + "\", \"retentionUntil\": \"" + str.getRetentionUntil() + "\"}");

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/ctr")
    public ResponseEntity<List<CurrencyTransactionReport>> listCtrs() {
        return ResponseEntity.ok(ctrRepository.findAll());
    }

    @PostMapping("/ctr/{id}/submit")
    @PreAuthorize("hasRole('OCOS')")
    public ResponseEntity<?> submitCtr(@PathVariable UUID id, Authentication authentication) {
        CurrencyTransactionReport ctr = ctrRepository.findById(id).orElse(null);
        if (ctr == null) return ResponseEntity.notFound().build();

        try {
            ctr.markAsSubmitted();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
        }
        CurrencyTransactionReport saved = ctrRepository.save(ctr);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.CTR_SUBMITTED, "currency_transaction_reports", ctr.getId(),
                "{\"retentionUntil\": \"" + ctr.getRetentionUntil() + "\"}");

        return ResponseEntity.ok(saved);
    }

    private void validateStrReadyForSubmission(SuspiciousActivityReport str) {
        if (str.getAccountHolder() == null || str.getAccountHolder().isBlank()
                || str.getSuspicionDescription() == null || str.getSuspicionDescription().isBlank()
                || str.getBusinessRelationshipDescription() == null
                || str.getBusinessRelationshipDescription().isBlank()
                || str.getReportingInstitution() == null || str.getReportingInstitution().isBlank()
                || str.getOcosName() == null || str.getOcosName().isBlank()) {
            throw new IllegalArgumentException("STR incompleto: preencha instituição, titular, descrição da suspeita, relação de negócio e OCOS");
        }
    }

    @GetMapping("/exports/str/{id}.pdf")
        @PreAuthorize("hasAnyRole('ADMIN', 'OCOS')")
    public ResponseEntity<byte[]> exportStrPdf(@PathVariable UUID id, Authentication authentication) {
            SuspiciousActivityReport report = strRepository.findById(id).orElse(null);
            if (report == null) return ResponseEntity.notFound().build();
            if (!report.isSubmitted()) {
                return ResponseEntity.badRequest().build();
            }
            byte[] content = regulatoryExportService.exportStrPdf(report);
            auditService.record((User) authentication.getPrincipal(), AuditAction.REGULATORY_EXPORT,
                    "suspicious_activity_reports", id, "{\"format\":\"PDF\"}");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"str-" + id + ".pdf\"")
                    .body(content);
    }

    @GetMapping("/exports/annual/{year}.xlsx")
        @PreAuthorize("hasAnyRole('ADMIN', 'OCOS')")
    public ResponseEntity<byte[]> exportAnnual(@PathVariable int year, Authentication authentication) throws java.io.IOException {
            if (year < 2000 || year > Year.now().getValue() + 1) {
                return ResponseEntity.badRequest().build();
            }
            byte[] content = regulatoryExportService.exportAnnualExcel(year);
            auditService.record((User) authentication.getPrincipal(), AuditAction.REGULATORY_EXPORT,
                    "annual_regulatory_reports", null, "{\"format\":\"XLSX\",\"year\":" + year + "}");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-bm-" + year + ".xlsx\"")
                    .body(content);
    }
}
