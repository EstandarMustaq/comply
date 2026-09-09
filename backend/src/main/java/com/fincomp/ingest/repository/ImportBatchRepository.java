package com.fincomp.ingest.repository;

import com.fincomp.ingest.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.time.Instant;
import java.util.List;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
    List<ImportBatch> findByImportedAtBefore(Instant cutoff);
}
