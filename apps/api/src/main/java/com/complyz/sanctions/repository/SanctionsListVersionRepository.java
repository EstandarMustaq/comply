package com.complyz.sanctions.repository;

import com.complyz.sanctions.entity.SanctionsListVersion;
import com.complyz.sanctions.entity.SanctionsSourceList;
import com.complyz.sanctions.entity.SanctionsVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SanctionsListVersionRepository extends JpaRepository<SanctionsListVersion, UUID> {
    Optional<SanctionsListVersion> findByVersion(String version);
    List<SanctionsListVersion> findBySourceListOrderByCreatedAtDesc(SanctionsSourceList sourceList);
    List<SanctionsListVersion> findByStatus(SanctionsVersionStatus status);
}
