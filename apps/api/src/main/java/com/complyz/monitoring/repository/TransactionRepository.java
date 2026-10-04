package com.complyz.monitoring.repository;

import com.complyz.monitoring.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.complyz.monitoring.entity.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.client.id = :clientId AND t.client.organization.id = :organizationId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByClientAndOrganizationAndDateBetween(
            @Param("clientId") UUID clientId,
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.client.id = :clientId AND t.client.organization.id = :organizationId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.type IN :types")
    BigDecimal sumAmountByClientAndOrganizationAndDateBetweenAndTypeIn(
            @Param("clientId") UUID clientId,
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("types") List<TransactionType> types
    );

    @Query("SELECT t FROM Transaction t WHERE t.client.id = :clientId " +
            "AND t.client.organization.id = :organizationId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.type IN :types ORDER BY t.transactionDate ASC")
    List<Transaction> findByClientAndOrganizationAndDateBetweenAndTypeIn(
            @Param("clientId") UUID clientId,
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("types") List<TransactionType> types
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization.id = :organizationId")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization.id = :organizationId " +
            "AND t.retentionUntil < :date")
    long countByOrganizationIdAndRetentionUntilBefore(
            @Param("organizationId") UUID organizationId,
            @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization.id = :organizationId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countByOrganizationAndTransactionDateBetween(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.client.organization.id = :organizationId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByOrganizationAndTransactionDateBetween(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
