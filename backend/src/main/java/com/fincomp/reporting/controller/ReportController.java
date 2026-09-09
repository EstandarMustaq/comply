package com.fincomp.reporting.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.reporting.entity.CurrencyTransactionReport;
import com.fincomp.reporting.entity.SuspiciousActivityReport;
import com.fincomp.reporting.repository.CurrencyTransactionReportRepository;
import com.fincomp.reporting.repository.SuspiciousActivityReportRepository;
import com.fincomp.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SuspiciousActivityReportRepository strRepository;
    private final CurrencyTransactionReportRepository ctrRepository;
    private final AuditService auditService;

    public ReportController(
            SuspiciousActivityReportRepository strRepository,
            CurrencyTransactionReportRepository ctrRepository,
            AuditService auditService
    ) {
        this.strRepository = strRepository;
        this.ctrRepository = ctrRepository;
        this.auditService = auditService;
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

        str.markAsSubmitted(request.gifimRegistrationNumber());
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

        ctr.markAsSubmitted();
        CurrencyTransactionReport saved = ctrRepository.save(ctr);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.CTR_SUBMITTED, "currency_transaction_reports", ctr.getId(),
                "{\"retentionUntil\": \"" + ctr.getRetentionUntil() + "\"}");

        return ResponseEntity.ok(saved);
    }
}
