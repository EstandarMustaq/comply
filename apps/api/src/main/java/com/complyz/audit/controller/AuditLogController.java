package com.complyz.audit.controller;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.entity.AuditLog;
import com.complyz.audit.service.AuditLogQueryService;
import com.complyz.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/audit-log")
@PreAuthorize("hasAnyRole('ADMIN', 'OCOS', 'AUDITOR_READONLY')")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    public AuditLogController(AuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLog>> listAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @ParameterObject Pageable pageable,
            Authentication authentication
    ) {
        User actor = (User) authentication.getPrincipal();
        Page<AuditLog> logs = auditLogQueryService.listForOrganization(actor, action, pageable);
        return ResponseEntity.ok(logs);
    }
}
