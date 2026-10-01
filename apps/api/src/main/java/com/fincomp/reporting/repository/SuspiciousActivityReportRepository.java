package com.fincomp.reporting.repository;

import com.fincomp.reporting.entity.SuspiciousActivityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface SuspiciousActivityReportRepository extends JpaRepository<SuspiciousActivityReport, UUID> {
    List<SuspiciousActivityReport> findBySubmittedAtIsNull();
    List<SuspiciousActivityReport> findBySubmittedAtIsNotNull();
    List<SuspiciousActivityReport> findBySubmittedAtBetween(Instant start, Instant end);
}
