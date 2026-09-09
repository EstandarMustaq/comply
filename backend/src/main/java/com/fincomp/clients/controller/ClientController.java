package com.fincomp.clients.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.clients.entity.Client;
import com.fincomp.clients.entity.DueDiligenceRegime;
import com.fincomp.clients.entity.RiskRating;
import com.fincomp.clients.repository.ClientRepository;
import com.fincomp.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository clientRepository;
    private final AuditService auditService;

    public ClientController(ClientRepository clientRepository, AuditService auditService) {
        this.clientRepository = clientRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<Page<Client>> listClients(
            @RequestParam(required = false) DueDiligenceRegime regime,
            @RequestParam(required = false) RiskRating risk,
            @RequestParam(required = false) Boolean pep,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        Specification<Client> spec = Specification.where(null);

        if (regime != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("dueDiligenceRegime"), regime));
        }
        if (risk != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("riskRating"), risk));
        }
        if (pep != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pep"), pep));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("idDocumentNumber")), pattern),
                    cb.like(cb.lower(root.get("nuib")), pattern)
            ));
        }

        Page<Client> clients = clientRepository.findAll(spec, pageable);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable UUID id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record UpdateRiskRatingRequest(@NotNull RiskRating riskRating) {}

    @PatchMapping("/{id}/risk-rating")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> updateRiskRating(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRiskRatingRequest request,
            Authentication authentication
    ) {
        Client client = clientRepository.findById(id).orElse(null);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        client.setRiskRating(request.riskRating());
        client.setRiskRatingUpdatedAt(LocalDate.now());
        clientRepository.save(client);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.CLIENT_UPDATED, "clients", client.getId(),
                "{\"field\": \"riskRating\", \"newValue\": \"" + request.riskRating() + "\"}");

        return ResponseEntity.ok(client);
    }

    @PatchMapping("/{id}/pep-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> registerPepReview(@PathVariable UUID id, Authentication authentication) {
        Client client = clientRepository.findById(id).orElse(null);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        client.setPepLastReviewedAt(LocalDate.now());
        clientRepository.save(client);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.PEP_REVIEWED, "clients", client.getId(),
                "{\"pepLastReviewedAt\": \"" + LocalDate.now() + "\"}");

        return ResponseEntity.ok(Map.of("message", "Revisão anual de PEP registrada com sucesso", "date", LocalDate.now()));
    }

    public record CreateClientRequest(
            @NotBlank String fullName,
            @NotBlank String idDocumentType,
            @NotBlank String idDocumentNumber,
            String nuib,
            DueDiligenceRegime dueDiligenceRegime,
            boolean isPep,
            String pepCategory,
            boolean seniorManagementApproved
    ) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> createClient(
            @Valid @RequestBody CreateClientRequest request,
            Authentication authentication
    ) {
        Client client = new Client(request.fullName(), request.idDocumentType(), request.idDocumentNumber());
        client.setNuib(request.nuib());
        if (request.dueDiligenceRegime() != null) {
            client.setDueDiligenceRegime(request.dueDiligenceRegime());
        }
        client.setPep(request.isPep());
        client.setPepCategory(request.pepCategory());
        client.setSeniorManagementApproved(request.seniorManagementApproved());
        if (request.isPep()) {
            client.setPepLastReviewedAt(LocalDate.now());
        }

        Client saved = clientRepository.save(client);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.CLIENT_UPDATED, "clients", saved.getId(), "{\"action\": \"CREATED\"}");

        return ResponseEntity.ok(saved);
    }
}
