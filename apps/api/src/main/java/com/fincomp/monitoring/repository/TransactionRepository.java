package com.fincomp.monitoring.repository;

import com.fincomp.monitoring.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.fincomp.monitoring.entity.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByClientId(UUID clientId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.client.id = :clientId AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByClientAndDateBetween(
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.client.id = :clientId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.type IN :types")
    BigDecimal sumAmountByClientAndDateBetweenAndTypeIn(
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("types") List<TransactionType> types
    );

    @Query("SELECT t FROM Transaction t WHERE t.client.id = :clientId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate ASC")
    List<Transaction> findByClientAndDateBetween(
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.client.id = :clientId AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.type IN :types ORDER BY t.transactionDate ASC")
    List<Transaction> findByClientAndDateBetweenAndTypeIn(
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("types") List<TransactionType> types
    );

    long countByRetentionUntilBefore(LocalDate date);

    long countByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTransactionDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
