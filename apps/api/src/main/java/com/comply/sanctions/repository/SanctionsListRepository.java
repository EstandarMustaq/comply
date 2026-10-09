package com.comply.sanctions.repository;

import com.comply.sanctions.entity.SanctionsListEntry;
import com.comply.sanctions.entity.SanctionsSourceList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SanctionsListRepository extends JpaRepository<SanctionsListEntry, UUID> {

  List<SanctionsListEntry> findBySourceList(SanctionsSourceList sourceList);

  List<SanctionsListEntry> findByVersionId(UUID versionId);

  @Query(
      "SELECT s FROM SanctionsListEntry s WHERE LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<SanctionsListEntry> searchByNameLike(@Param("query") String query);
}
