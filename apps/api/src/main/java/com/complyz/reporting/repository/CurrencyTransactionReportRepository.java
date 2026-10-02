package com.complyz.reporting.repository;

import com.complyz.reporting.entity.CurrencyTransactionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@Repository
public interface CurrencyTransactionReportRepository extends JpaRepository<CurrencyTransactionReport, UUID> {
    List<CurrencyTransactionReport> findBySubmittedAtIsNull();

    boolean existsByClientIdAndAggregationPeriodStartAndAggregationPeriodEndAndThresholdRuleId(
            UUID clientId,
            LocalDate aggregationPeriodStart,
            LocalDate aggregationPeriodEnd,
            UUID thresholdRuleId
    );

    List<CurrencyTransactionReport> findByAggregationPeriodStartBetween(LocalDate start, LocalDate end);

    @Query("""
            SELECT report FROM CurrencyTransactionReport report
            WHERE report.client.organization.id = :organizationId
              AND report.aggregationPeriodStart BETWEEN :start AND :end
            """)
    List<CurrencyTransactionReport> findByOrganizationAndAggregationPeriodStartBetween(
            @Param("organizationId") UUID organizationId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
