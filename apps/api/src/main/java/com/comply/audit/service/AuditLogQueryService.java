package com.comply.audit.service;

import com.comply.audit.entity.AuditAction;
import com.comply.audit.entity.AuditLog;
import com.comply.audit.repository.AuditLogRepository;
import com.comply.tenancy.service.TenantAccessService;
import com.comply.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogQueryService {

  private final AuditLogRepository auditLogRepository;
  private final TenantAccessService tenantAccessService;

  public AuditLogQueryService(
      AuditLogRepository auditLogRepository, TenantAccessService tenantAccessService) {
    this.auditLogRepository = auditLogRepository;
    this.tenantAccessService = tenantAccessService;
  }

  @Transactional(readOnly = true)
  public Page<AuditLog> listForOrganization(User actor, AuditAction action, Pageable pageable) {
    return auditLogRepository.findByOrganizationIdAndAction(
        tenantAccessService.organizationId(actor), action, pageable);
  }
}
