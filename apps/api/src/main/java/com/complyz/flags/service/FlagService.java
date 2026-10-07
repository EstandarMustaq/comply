package com.complyz.flags.service;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.service.AuditService;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.reporting.entity.SuspiciousActivityReport;
import com.complyz.reporting.repository.SuspiciousActivityReportRepository;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FlagService {

    private final FlagRepository flagRepository;
    private final SuspiciousActivityReportRepository strRepository;
    private final AuditService auditService;
    private final TenantAccessService tenantAccessService;

    public FlagService(
            FlagRepository flagRepository,
            SuspiciousActivityReportRepository strRepository,
            AuditService auditService,
            TenantAccessService tenantAccessService
    ) {
        this.flagRepository = flagRepository;
        this.strRepository = strRepository;
        this.auditService = auditService;
        this.tenantAccessService = tenantAccessService;
    }

    public List<Flag> listFlags(User actor, FlagStatus status, FlagType type) {
        UUID organizationId = requireOrganizationId(actor);
        Sort sort = Sort.by(Sort.Order.asc("slaDeadline").nullsLast(), Sort.Order.desc("createdAt"));
        return flagRepository.findAllByOrganizationId(organizationId, status, type, sort);
    }

    @Transactional
    public Flag reviewFlag(UUID flagId, User reviewer, String note, FlagStatus nextStatus) {
        UUID organizationId = requireOrganizationId(reviewer);
        Flag flag = flagRepository.findByIdAndOrganizationId(flagId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + flagId));

        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(Instant.now());
        flag.setReviewNote(note);
        flag.setStatus(nextStatus != null ? nextStatus : FlagStatus.RESOLVED);

        Flag saved = flagRepository.save(flag);

        auditService.record(reviewer, AuditAction.FLAG_REVIEWED, "flags", flag.getId(),
                "{\"status\": \"" + saved.getStatus() + "\", \"note\": \"" + note + "\"}");

        return saved;
    }

    @Transactional
    public SuspiciousActivityReport escalateToStr(UUID flagId, User ocos, String institutionName) {
        UUID organizationId = requireOrganizationId(ocos);
        Flag flag = flagRepository.findByIdAndOrganizationId(flagId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + flagId));

        flag.setStatus(FlagStatus.ESCALATED_TO_STR);
        flag.setReviewedBy(ocos);
        flag.setReviewedAt(Instant.now());
        flagRepository.save(flag);

        SuspiciousActivityReport str = new SuspiciousActivityReport();
        str.setFlag(flag);
        str.setReportingInstitution(institutionName != null ? institutionName : "Microfinance institution");
        str.setAccountHolder(flag.getClient() != null ? flag.getClient().getFullName() : "Unidentified");
        if (flag.getTransaction() != null) {
            str.setMonetaryValue(flag.getTransaction().getAmount());
        }
        str.setSuspicionDescription("Escalated from alert: " + flag.getReason());
        str.setBusinessRelationshipDescription("Customer is registered in Complyz.");
        str.setOcosName(ocos.getFullName());

        SuspiciousActivityReport savedStr = strRepository.save(str);

        auditService.record(ocos, AuditAction.FLAG_REVIEWED, "flags", flag.getId(),
                "{\"escalatedTo\": \"STR\", \"strId\": \"" + savedStr.getId() + "\"}");

        return savedStr;
    }

    private UUID requireOrganizationId(User actor) {
        UUID organizationId = tenantAccessService.organizationId(actor);
        if (organizationId == null) {
            throw new IllegalStateException("The user's Organization is required.");
        }
        return organizationId;
    }
}
