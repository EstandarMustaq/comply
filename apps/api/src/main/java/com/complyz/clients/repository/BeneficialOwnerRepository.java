package com.complyz.clients.repository;

import com.complyz.clients.entity.BeneficialOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficialOwnerRepository extends JpaRepository<BeneficialOwner, UUID> {

    @Query("SELECT owner FROM BeneficialOwner owner WHERE owner.client.id = :clientId " +
            "AND owner.client.organization.id = :organizationId")
    List<BeneficialOwner> findByClientIdAndClientOrganizationId(
            @Param("clientId") UUID clientId,
            @Param("organizationId") UUID organizationId
    );

    @Query("SELECT owner FROM BeneficialOwner owner WHERE owner.id = :ownerId " +
            "AND owner.client.organization.id = :organizationId")
    Optional<BeneficialOwner> findByIdAndClientOrganizationId(
            @Param("ownerId") UUID ownerId,
            @Param("organizationId") UUID organizationId
    );
}
