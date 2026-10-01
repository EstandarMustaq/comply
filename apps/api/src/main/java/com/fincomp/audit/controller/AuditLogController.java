package com.fincomp.audit.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.entity.AuditLog;
import com.fincomp.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-log")
@PreAuthorize("hasAnyRole('ADMIN', 'OCOS', 'AUDITOR_READONLY')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLog>> listAuditLogs(
            @RequestParam(required = false) AuditAction action,
            Pageable pageable
    ) {
        if (action != null) {
            return ResponseEntity.ok(auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable));
        }
        return ResponseEntity.ok(auditLogRepository.findByOrderByCreatedAtDesc(pageable));
    }
}
