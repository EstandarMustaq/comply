package com.fincomp.flags.repository;

import com.fincomp.flags.entity.Flag;
import com.fincomp.flags.entity.FlagStatus;
import com.fincomp.flags.entity.FlagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlagRepository extends JpaRepository<Flag, UUID>, JpaSpecificationExecutor<Flag> {

    List<Flag> findByStatus(FlagStatus status);

    List<Flag> findByStatusOrderBySlaDeadlineAsc(FlagStatus status);

    List<Flag> findByTypeAndStatus(FlagType type, FlagStatus status);

    boolean existsByClientIdAndTypeAndStatus(UUID clientId, FlagType type, FlagStatus status);

    boolean existsByTransactionIdAndType(UUID transactionId, FlagType type);

    long countByStatus(FlagStatus status);

    long countByType(FlagType type);

    @Query("SELECT COUNT(f) FROM Flag f WHERE f.status = 'PENDING' AND f.slaDeadline IS NOT NULL AND f.slaDeadline < :now")
    long countOverdueSla(Instant now);
}
