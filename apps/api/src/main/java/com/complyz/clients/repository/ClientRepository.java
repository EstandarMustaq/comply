package com.complyz.clients.repository;

import com.complyz.clients.entity.Client;
import com.complyz.clients.entity.DueDiligenceRegime;
import com.complyz.tenancy.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {
    Optional<Client> findByOrganizationAndIdDocumentNumber(Organization organization, String idDocumentNumber);
    Optional<Client> findByNuib(String nuib);
    List<Client> findByDueDiligenceRegime(DueDiligenceRegime regime);
    List<Client> findByPepTrueAndPepLastReviewedAtBeforeOrPepTrueAndPepLastReviewedAtIsNull(LocalDate date);
    @Query("SELECT COUNT(c) FROM Client c WHERE c.organization.id = :organizationId")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.organization.id = :organizationId AND c.kycComplete = true")
    long countByOrganizationIdAndKycCompleteTrue(@Param("organizationId") UUID organizationId);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.organization.id = :organizationId AND c.dueDiligenceRegime = :regime")
    long countByOrganizationIdAndDueDiligenceRegime(
            @Param("organizationId") UUID organizationId,
            @Param("regime") DueDiligenceRegime regime
    );
    org.springframework.data.domain.Page<Client> findAllByOrganization(Organization organization, org.springframework.data.domain.Pageable pageable);
    Optional<Client> findByIdAndOrganization(java.util.UUID id, Organization organization);
}
