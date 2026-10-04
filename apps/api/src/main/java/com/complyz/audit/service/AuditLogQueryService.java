package com.complyz.audit.service;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.entity.AuditLog;
import com.complyz.audit.repository.AuditLogRepository;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final TenantAccessService tenantAccessService;

    public AuditLogQueryService(
            AuditLogRepository auditLogRepository,
            TenantAccessService tenantAccessService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listForOrganization(User actor, AuditAction action, Pageable pageable) {
        return auditLogRepository.findByOrganizationIdAndAction(
                tenantAccessService.organizationId(actor), action, pageable);
    }
}
