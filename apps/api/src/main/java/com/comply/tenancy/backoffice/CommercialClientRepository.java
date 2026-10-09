package com.comply.tenancy.backoffice;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Platform back-office persistence only. Customer authorization must never use this repository. */
public interface CommercialClientRepository extends Repository<CommercialClient, UUID> {
  /**
   * Persists back-office commercial metadata without changing tenant authorization.
   *
   * @param commercialClient commercial metadata to persist
   * @return the saved entity; use this instance for subsequent changes
   */
  CommercialClient save(CommercialClient commercialClient);

  /**
   * Finds commercial metadata by UUID for platform back-office use only.
   *
   * @param id persistent record UUID
   * @return the matching record, or empty when no record matches the query
   */
  Optional<CommercialClient> findById(UUID id);
}
