package com.fincomp.clients.repository;

import com.fincomp.clients.entity.Client;
import com.fincomp.clients.entity.DueDiligenceRegime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {
    Optional<Client> findByIdDocumentNumber(String idDocumentNumber);
    Optional<Client> findByNuib(String nuib);
    List<Client> findByDueDiligenceRegime(DueDiligenceRegime regime);
    List<Client> findByPepTrueAndPepLastReviewedAtBefore(LocalDate date);
    long countByKycCompleteTrue();
    long countByDueDiligenceRegime(DueDiligenceRegime regime);
}
