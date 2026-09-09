package com.fincomp.audit.service;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.entity.AuditLog;
import com.fincomp.audit.repository.AuditLogRepository;
import com.fincomp.users.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, AuditAction action, String entity, UUID entityId, String detailsJson) {
        AuditLog entry = new AuditLog(actor, action, entity, entityId, detailsJson);
        auditLogRepository.save(entry);
    }
}
