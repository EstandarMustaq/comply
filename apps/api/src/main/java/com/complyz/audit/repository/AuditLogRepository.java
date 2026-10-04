package com.complyz.audit.repository;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    @Query(value = "SELECT log FROM AuditLog log " +
            "WHERE log.actor.organization.id = :organizationId " +
            "AND (:action IS NULL OR log.action = :action) " +
            "ORDER BY log.createdAt DESC",
            countQuery = "SELECT COUNT(log) FROM AuditLog log " +
                    "WHERE log.actor.organization.id = :organizationId " +
                    "AND (:action IS NULL OR log.action = :action)")
    Page<AuditLog> findByOrganizationIdAndAction(
            @Param("organizationId") UUID organizationId,
            @Param("action") AuditAction action,
            Pageable pageable
    );
}
