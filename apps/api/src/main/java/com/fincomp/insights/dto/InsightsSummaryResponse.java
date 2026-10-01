package com.fincomp.insights.dto;

public record InsightsSummaryResponse(
        long totalClients,
        long kycCompleteCount,
        double kycCompletenessPercentage,
        long simplifiedRegimeCount,
        long normalRegimeCount,
        long pendingFlagsCount,
        long sanctionsMatchCount,
        long overdueSlaCount,
        long pepReviewDueCount,
        long totalTransactionsCount,
        long retentionExpiringCount
) {}
