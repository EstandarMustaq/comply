package com.complyz.insights.service;

import com.complyz.clients.entity.DueDiligenceRegime;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.insights.dto.InsightsSummaryResponse;
import com.complyz.monitoring.repository.TransactionRepository;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class InsightsService {

    private final ClientRepository clientRepository;
    private final FlagRepository flagRepository;
    private final TransactionRepository transactionRepository;
    private final TenantAccessService tenantAccessService;

    public InsightsService(
            ClientRepository clientRepository,
            FlagRepository flagRepository,
            TransactionRepository transactionRepository,
            TenantAccessService tenantAccessService
    ) {
        this.clientRepository = clientRepository;
        this.flagRepository = flagRepository;
        this.transactionRepository = transactionRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Cacheable(value = "insights_cache", key = "'summary:' + #p0.organization.id")
    public InsightsSummaryResponse getSummary(User actor) {
        UUID organizationId = tenantAccessService.organizationId(actor);
        if (organizationId == null) {
            throw new IllegalStateException("Utilizador não associado a uma organização");
        }

        long totalClients = clientRepository.countByOrganizationId(organizationId);
        long kycComplete = clientRepository.countByOrganizationIdAndKycCompleteTrue(organizationId);
        double kycPercentage = totalClients > 0 ? ((double) kycComplete / totalClients) * 100.0 : 0.0;

        long simplifiedCount = clientRepository.countByOrganizationIdAndDueDiligenceRegime(
                organizationId, DueDiligenceRegime.SIMPLIFIED);
        long normalCount = clientRepository.countByOrganizationIdAndDueDiligenceRegime(
                organizationId, DueDiligenceRegime.NORMAL);

        long pendingFlags = flagRepository.countByOrganizationIdAndStatus(organizationId, FlagStatus.PENDING);
        long sanctionsCount = flagRepository.countByOrganizationIdAndType(organizationId, FlagType.SANCTIONS_MATCH);
        long overdueSla = flagRepository.countOverdueSlaByOrganizationId(
                organizationId, FlagStatus.PENDING, Instant.now());
        long pepReviewDue = flagRepository.countByOrganizationIdAndType(organizationId, FlagType.PEP_REVIEW_DUE);

        long totalTx = transactionRepository.countByOrganizationId(organizationId);
        long retentionExpiring = transactionRepository.countByOrganizationIdAndRetentionUntilBefore(
                organizationId, LocalDate.now().plusMonths(3));

        return new InsightsSummaryResponse(
                totalClients,
                kycComplete,
                Math.round(kycPercentage * 100.0) / 100.0,
                simplifiedCount,
                normalCount,
                pendingFlags,
                sanctionsCount,
                overdueSla,
                pepReviewDue,
                totalTx,
                retentionExpiring
        );
    }

    @CacheEvict(value = "insights_cache", allEntries = true)
    public void evictInsightsCache() {
        // Evict cached insights after imported data or alert status changes.
    }
}
