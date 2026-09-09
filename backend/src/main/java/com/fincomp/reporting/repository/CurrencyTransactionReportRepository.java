package com.fincomp.reporting.repository;

import com.fincomp.reporting.entity.CurrencyTransactionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CurrencyTransactionReportRepository extends JpaRepository<CurrencyTransactionReport, UUID> {
    List<CurrencyTransactionReport> findBySubmittedAtIsNull();
}
