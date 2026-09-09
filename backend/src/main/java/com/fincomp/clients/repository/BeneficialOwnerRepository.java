package com.fincomp.clients.repository;

import com.fincomp.clients.entity.BeneficialOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BeneficialOwnerRepository extends JpaRepository<BeneficialOwner, UUID> {
    List<BeneficialOwner> findByClientId(UUID clientId);
}
