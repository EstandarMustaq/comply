package com.complyz.tenancy.backoffice;

import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;

/** Platform back-office persistence only. Customer authorization must never use this repository. */
public interface CommercialClientRepository extends Repository<CommercialClient, UUID> {
    CommercialClient save(CommercialClient commercialClient);
    Optional<CommercialClient> findById(UUID id);
}
