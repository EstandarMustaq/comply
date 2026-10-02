package com.complyz.reporting.repository;

import com.complyz.reporting.entity.SuspiciousActivityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface SuspiciousActivityReportRepository extends JpaRepository<SuspiciousActivityReport, UUID> {
    List<SuspiciousActivityReport> findBySubmittedAtIsNull();
    List<SuspiciousActivityReport> findBySubmittedAtIsNotNull();
    List<SuspiciousActivityReport> findBySubmittedAtBetween(Instant start, Instant end);

    @Query("""
            SELECT report FROM SuspiciousActivityReport report
            WHERE report.id = :reportId
              AND report.flag.client.organization.id = :organizationId
            """)
    Optional<SuspiciousActivityReport> findByIdAndOrganizationId(
            @Param("reportId") UUID reportId,
            @Param("organizationId") UUID organizationId
    );

    @Query("""
            SELECT report FROM SuspiciousActivityReport report
            WHERE report.submittedAt BETWEEN :start AND :end
              AND report.flag.client.organization.id = :organizationId
            """)
    List<SuspiciousActivityReport> findSubmittedBetweenForOrganization(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("organizationId") UUID organizationId
    );
}
