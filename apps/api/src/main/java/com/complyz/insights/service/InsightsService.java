package com.complyz.insights.service;

import com.complyz.clients.entity.DueDiligenceRegime;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.insights.dto.InsightsSummaryResponse;
import com.complyz.monitoring.repository.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class InsightsService {

    private final ClientRepository clientRepository;
    private final FlagRepository flagRepository;
    private final TransactionRepository transactionRepository;

    public InsightsService(
            ClientRepository clientRepository,
            FlagRepository flagRepository,
            TransactionRepository transactionRepository
    ) {
        this.clientRepository = clientRepository;
        this.flagRepository = flagRepository;
        this.transactionRepository = transactionRepository;
    }

    @Cacheable(value = "insights_cache", key = "'summary'")
    public InsightsSummaryResponse getSummary() {
        long totalClients = clientRepository.count();
        long kycComplete = clientRepository.countByKycCompleteTrue();
        double kycPercentage = totalClients > 0 ? ((double) kycComplete / totalClients) * 100.0 : 0.0;

        long simplifiedCount = clientRepository.countByDueDiligenceRegime(DueDiligenceRegime.SIMPLIFIED);
        long normalCount = clientRepository.countByDueDiligenceRegime(DueDiligenceRegime.NORMAL);

        long pendingFlags = flagRepository.countByStatus(FlagStatus.PENDING);
        long sanctionsCount = flagRepository.countByType(FlagType.SANCTIONS_MATCH);
        long overdueSla = flagRepository.countOverdueSla(Instant.now());
        long pepReviewDue = flagRepository.countByType(FlagType.PEP_REVIEW_DUE);

        long totalTx = transactionRepository.count();
        long retentionExpiring = transactionRepository.countByRetentionUntilBefore(LocalDate.now().plusMonths(3));

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
