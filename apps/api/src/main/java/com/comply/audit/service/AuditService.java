package com.comply.audit.service;

import com.comply.audit.entity.AuditAction;
import com.comply.audit.entity.AuditLog;
import com.comply.audit.repository.AuditLogRepository;
import com.comply.users.entity.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      User actor, AuditAction action, String entity, UUID entityId, String detailsJson) {
    AuditLog entry = new AuditLog(actor, action, entity, entityId, detailsJson);
    auditLogRepository.save(entry);
  }
}
