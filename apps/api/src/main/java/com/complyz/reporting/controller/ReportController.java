package com.complyz.reporting.controller;

import com.complyz.api.ApiErrorBodies;
import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.service.AuditService;
import com.complyz.reporting.entity.CurrencyTransactionReport;
import com.complyz.reporting.entity.SuspiciousActivityReport;
import com.complyz.reporting.repository.CurrencyTransactionReportRepository;
import com.complyz.reporting.repository.SuspiciousActivityReportRepository;
import com.complyz.reporting.service.ReportExportService;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Year;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final SuspiciousActivityReportRepository strRepository;
    private final CurrencyTransactionReportRepository ctrRepository;
    private final AuditService auditService;
    private final ReportExportService reportExportService;
    private final TenantAccessService tenantAccessService;

    public ReportController(
            SuspiciousActivityReportRepository strRepository,
            CurrencyTransactionReportRepository ctrRepository,
            AuditService auditService,
            ReportExportService reportExportService,
            TenantAccessService tenantAccessService
    ) {
        this.strRepository = strRepository;
        this.ctrRepository = ctrRepository;
        this.auditService = auditService;
        this.reportExportService = reportExportService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/str")
    public ResponseEntity<List<SuspiciousActivityReport>> listStrs(
            @RequestParam(required = false) Boolean submitted,
            Authentication authentication
    ) {
        UUID organizationId = tenantAccessService.organizationId((User) authentication.getPrincipal());
        if (Boolean.TRUE.equals(submitted)) {
            return ResponseEntity.ok(strRepository.findSubmittedByOrganizationId(organizationId));
        }
        if (Boolean.FALSE.equals(submitted)) {
            return ResponseEntity.ok(strRepository.findDraftsByOrganizationId(organizationId));
        }
        return ResponseEntity.ok(strRepository.findAllByOrganizationId(organizationId));
    }

    @GetMapping("/str/{id}")
    public ResponseEntity<?> getStr(@PathVariable UUID id, Authentication authentication) {
        UUID organizationId = tenantAccessService.organizationId((User) authentication.getPrincipal());
        return strRepository.findByIdAndOrganizationId(id, organizationId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(ApiErrorBodies.notFound()));
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
        UUID organizationId = tenantAccessService.organizationId((User) authentication.getPrincipal());
        SuspiciousActivityReport str = strRepository.findByIdAndOrganizationId(id, organizationId).orElse(null);
        if (str == null) return ResponseEntity.status(404).body(ApiErrorBodies.notFound());

        if (str.getSubmittedAt() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Relatório já submetido não pode ser editado"));
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
        User actor = (User) authentication.getPrincipal();
        UUID organizationId = tenantAccessService.organizationId(actor);
        SuspiciousActivityReport str = strRepository.findByIdAndOrganizationId(id, organizationId).orElse(null);
        if (str == null) return ResponseEntity.status(404).body(ApiErrorBodies.notFound());

        try {
            validateStrReadyForSubmission(str);
            str.markAsSubmitted(request.gifimRegistrationNumber());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
        }
        SuspiciousActivityReport saved = strRepository.save(str);

        auditService.record(actor, AuditAction.STR_SUBMITTED, "suspicious_activity_reports", str.getId(),
                "{\"gifimRegistrationNumber\": \"" + request.gifimRegistrationNumber() + "\", \"retentionUntil\": \"" + str.getRetentionUntil() + "\"}");

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/ctr")
    public ResponseEntity<List<CurrencyTransactionReport>> listCtrs(Authentication authentication) {
        UUID organizationId = tenantAccessService.organizationId((User) authentication.getPrincipal());
        return ResponseEntity.ok(ctrRepository.findAllByClientOrganizationId(organizationId));
    }

    @PostMapping("/ctr/{id}/submit")
    @PreAuthorize("hasRole('OCOS')")
    public ResponseEntity<?> submitCtr(@PathVariable UUID id, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        UUID organizationId = tenantAccessService.organizationId(actor);
        CurrencyTransactionReport ctr = ctrRepository.findByIdAndClientOrganizationId(id, organizationId).orElse(null);
        if (ctr == null) return ResponseEntity.status(404).body(ApiErrorBodies.notFound());

        try {
            ctr.markAsSubmitted();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
        }
        CurrencyTransactionReport saved = ctrRepository.save(ctr);

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
    public ResponseEntity<?> exportStrPdf(@PathVariable UUID id, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        UUID organizationId = tenantAccessService.organizationId(actor);
        SuspiciousActivityReport report = strRepository.findByIdAndOrganizationId(id, organizationId).orElse(null);
        if (report == null) {
            return ResponseEntity.status(404).body(ApiErrorBodies.notFound());
        }
        if (!report.isSubmitted()) {
            return ResponseEntity.badRequest().body(ApiErrorBodies.invalidExportState());
        }
        byte[] content = reportExportService.exportStrPdf(report);
        auditService.record(actor, AuditAction.REGULATORY_EXPORT,
                "suspicious_activity_reports", id, "{\"format\":\"PDF\"}");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"str-" + id + ".pdf\"")
                .body(content);
    }

    @GetMapping("/exports/annual/{year}.xlsx")
    @PreAuthorize("hasAnyRole('ADMIN', 'OCOS')")
    public ResponseEntity<?> exportAnnual(@PathVariable int year, Authentication authentication) throws IOException {
        if (year < 2000 || year > Year.now().getValue() + 1) {
            return ResponseEntity.badRequest().body(ApiErrorBodies.invalidExportPeriod());
        }
        User actor = (User) authentication.getPrincipal();
        byte[] content = reportExportService.exportAnnualExcel(
                year, tenantAccessService.organizationId(actor));
        auditService.record(actor, AuditAction.REGULATORY_EXPORT,
                "annual_reports", null, "{\"format\":\"XLSX\",\"year\":" + year + "}");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-bm-" + year + ".xlsx\"")
                .body(content);
    }
}
