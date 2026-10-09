package com.comply.tenancy.backoffice;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Platform back-office persistence only. Customer authorization must never use this repository. */
public interface CommercialClientRepository extends Repository<CommercialClient, UUID> {
  CommercialClient save(CommercialClient commercialClient);

  Optional<CommercialClient> findById(UUID id);
}
