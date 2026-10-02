package com.complyz.flags.service;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.service.AuditService;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.reporting.entity.SuspiciousActivityReport;
import com.complyz.reporting.repository.SuspiciousActivityReportRepository;
import com.complyz.users.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    public FlagService(
            FlagRepository flagRepository,
            SuspiciousActivityReportRepository strRepository,
            AuditService auditService
    ) {
        this.flagRepository = flagRepository;
        this.strRepository = strRepository;
        this.auditService = auditService;
    }

    public List<Flag> listFlags(FlagStatus status, FlagType type) {
        Specification<Flag> spec = Specification.unrestricted();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        // Sort by earliest deadline, then by newest creation time.
        Sort sort = Sort.by(Sort.Order.asc("slaDeadline").nullsLast(), Sort.Order.desc("createdAt"));
        return flagRepository.findAll(spec, sort);
    }

    @Transactional
    public Flag reviewFlag(UUID flagId, User reviewer, String note, FlagStatus nextStatus) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Flag não encontrada: " + flagId));

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
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Flag não encontrada: " + flagId));

        flag.setStatus(FlagStatus.ESCALATED_TO_STR);
        flag.setReviewedBy(ocos);
        flag.setReviewedAt(Instant.now());
        flagRepository.save(flag);

        SuspiciousActivityReport str = new SuspiciousActivityReport();
        str.setFlag(flag);
        str.setReportingInstitution(institutionName != null ? institutionName : "Instituição de Microfinanças");
        str.setAccountHolder(flag.getClient() != null ? flag.getClient().getFullName() : "Não identificado");
        if (flag.getTransaction() != null) {
            str.setMonetaryValue(flag.getTransaction().getAmount());
        }
        str.setSuspicionDescription("Escalado a partir da flag: " + flag.getReason());
        str.setBusinessRelationshipDescription("Cliente cadastrado no sistema Complyz");
        str.setOcosName(ocos.getFullName());

        SuspiciousActivityReport savedStr = strRepository.save(str);

        auditService.record(ocos, AuditAction.FLAG_REVIEWED, "flags", flag.getId(),
                "{\"escalatedTo\": \"STR\", \"strId\": \"" + savedStr.getId() + "\"}");

        return savedStr;
    }
}
