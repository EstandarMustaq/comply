package com.complyz.ingest.repository;

import com.complyz.ingest.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
    List<ImportBatch> findByImportedAtBefore(Instant cutoff);

    @Query("SELECT batch FROM ImportBatch batch LEFT JOIN batch.importedBy uploader " +
            "WHERE batch.organization.id = :organizationId " +
            "AND (uploader IS NULL OR uploader.organization.id = :organizationId) " +
            "ORDER BY batch.importedAt DESC")
    List<ImportBatch> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT batch FROM ImportBatch batch LEFT JOIN batch.importedBy uploader " +
            "WHERE batch.id = :batchId " +
            "AND batch.organization.id = :organizationId " +
            "AND (uploader IS NULL OR uploader.organization.id = :organizationId)")
    Optional<ImportBatch> findByIdAndOrganizationId(
            @Param("batchId") UUID batchId,
            @Param("organizationId") UUID organizationId
    );
}
