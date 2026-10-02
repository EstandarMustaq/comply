package com.complyz.audit.service;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.entity.AuditLog;
import com.complyz.audit.repository.AuditLogRepository;
import com.complyz.users.entity.User;
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
