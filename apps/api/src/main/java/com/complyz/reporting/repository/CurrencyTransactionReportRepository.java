package com.complyz.reporting.repository;

import com.complyz.reporting.entity.CurrencyTransactionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

@Repository
public interface CurrencyTransactionReportRepository extends JpaRepository<CurrencyTransactionReport, UUID> {

    @Query("SELECT COUNT(report) FROM CurrencyTransactionReport report " +
            "WHERE report.client.id = :clientId AND report.client.organization.id = :organizationId " +
            "AND report.aggregationPeriodStart = :start AND report.aggregationPeriodEnd = :end " +
            "AND report.thresholdRule.id = :thresholdRuleId")
    long countByClientIdAndOrganizationIdAndAggregationPeriodStartAndAggregationPeriodEndAndThresholdRuleId(
            @Param("clientId") UUID clientId,
            @Param("organizationId") UUID organizationId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("thresholdRuleId") UUID thresholdRuleId
    );


    @Query("SELECT report FROM CurrencyTransactionReport report " +
            "WHERE report.client.organization.id = :organizationId ORDER BY report.aggregationPeriodStart DESC")
    List<CurrencyTransactionReport> findAllByClientOrganizationId(
            @Param("organizationId") UUID organizationId
    );

    @Query("SELECT report FROM CurrencyTransactionReport report WHERE report.id = :reportId " +
            "AND report.client.organization.id = :organizationId")
    Optional<CurrencyTransactionReport> findByIdAndClientOrganizationId(
            @Param("reportId") UUID reportId,
            @Param("organizationId") UUID organizationId
    );

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
