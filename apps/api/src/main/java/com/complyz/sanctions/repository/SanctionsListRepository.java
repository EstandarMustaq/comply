package com.complyz.sanctions.repository;

import com.complyz.sanctions.entity.SanctionsListEntry;
import com.complyz.sanctions.entity.SanctionsSourceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SanctionsListRepository extends JpaRepository<SanctionsListEntry, UUID> {

    List<SanctionsListEntry> findBySourceList(SanctionsSourceList sourceList);
    List<SanctionsListEntry> findByVersionId(UUID versionId);

    @Query("SELECT s FROM SanctionsListEntry s WHERE LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<SanctionsListEntry> searchByNameLike(@Param("query") String query);
}
