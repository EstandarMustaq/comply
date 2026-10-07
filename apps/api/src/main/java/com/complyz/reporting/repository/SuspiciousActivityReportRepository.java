package com.complyz.reporting.repository;

import com.complyz.reporting.entity.SuspiciousActivityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuspiciousActivityReportRepository extends JpaRepository<SuspiciousActivityReport, UUID> {

    @Query("""
            SELECT report FROM SuspiciousActivityReport report
            LEFT JOIN report.flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            LEFT JOIN flagEntity.reviewedBy reviewer
            WHERE (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            )
              AND (reviewer IS NULL OR reviewer.organization.id = :organizationId)
            ORDER BY report.id DESC
            """)
    List<SuspiciousActivityReport> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
            SELECT report FROM SuspiciousActivityReport report
            LEFT JOIN report.flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            LEFT JOIN flagEntity.reviewedBy reviewer
            WHERE report.submittedAt IS NOT NULL AND (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            )
              AND (reviewer IS NULL OR reviewer.organization.id = :organizationId)
            ORDER BY report.id DESC
            """)
    List<SuspiciousActivityReport> findSubmittedByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
            SELECT report FROM SuspiciousActivityReport report
            LEFT JOIN report.flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            LEFT JOIN flagEntity.reviewedBy reviewer
            WHERE report.submittedAt IS NULL AND (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            )
              AND (reviewer IS NULL OR reviewer.organization.id = :organizationId)
            ORDER BY report.id DESC
            """)
    List<SuspiciousActivityReport> findDraftsByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
            SELECT report FROM SuspiciousActivityReport report
            LEFT JOIN report.flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            LEFT JOIN flagEntity.reviewedBy reviewer
            WHERE report.id = :reportId AND (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            )
              AND (reviewer IS NULL OR reviewer.organization.id = :organizationId)
            """)
    Optional<SuspiciousActivityReport> findByIdAndOrganizationId(
            @Param("reportId") UUID reportId,
            @Param("organizationId") UUID organizationId
    );

    @Query("""
            SELECT report FROM SuspiciousActivityReport report
            LEFT JOIN report.flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            LEFT JOIN flagEntity.reviewedBy reviewer
            WHERE report.submittedAt BETWEEN :start AND :end AND (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            )
              AND (reviewer IS NULL OR reviewer.organization.id = :organizationId)
            """)
    List<SuspiciousActivityReport> findSubmittedBetweenForOrganization(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("organizationId") UUID organizationId
    );
}
