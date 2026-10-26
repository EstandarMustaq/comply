package com.comply.sanctions.repository;

import com.comply.sanctions.entity.SanctionsListVersion;
import com.comply.sanctions.entity.SanctionsSourceList;
import com.comply.sanctions.entity.SanctionsVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionsListVersionRepository extends JpaRepository<SanctionsListVersion, UUID> {
  Optional<SanctionsListVersion> findByVersion(String version);

  List<SanctionsListVersion> findBySourceListOrderByCreatedAtDesc(SanctionsSourceList sourceList);

  List<SanctionsListVersion> findByStatus(SanctionsVersionStatus status);
}
