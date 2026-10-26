package com.comply.clients.controller;

import com.comply.api.ApiErrorBodies;
import com.comply.audit.entity.AuditAction;
import com.comply.audit.service.AuditService;
import com.comply.clients.entity.Client;
import com.comply.clients.entity.ClientType;
import com.comply.clients.entity.DueDiligenceRegime;
import com.comply.clients.entity.RiskRating;
import com.comply.clients.repository.ClientRepository;
import com.comply.tenancy.entity.Organization;
import com.comply.tenancy.service.TenantAccessService;
import com.comply.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ClientController {

  private final ClientRepository clientRepository;
  private final AuditService auditService;
  private final TenantAccessService tenantAccessService;

  public ClientController(
      ClientRepository clientRepository,
      AuditService auditService,
      TenantAccessService tenantAccessService) {
    this.clientRepository = clientRepository;
    this.auditService = auditService;
    this.tenantAccessService = tenantAccessService;
  }

  @GetMapping
  public ResponseEntity<Page<Client>> listClients(
      @RequestParam(required = false) DueDiligenceRegime regime,
      @RequestParam(required = false) RiskRating risk,
      @RequestParam(required = false) Boolean pep,
      @RequestParam(required = false) String search,
      @ParameterObject Pageable pageable,
      Authentication authentication) {
    Organization organization =
        tenantAccessService.requireOrganization((User) authentication.getPrincipal());
    Specification<Client> spec = Specification.unrestricted();
    spec = spec.and((root, query, cb) -> cb.equal(root.get("organization"), organization));

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
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("fullName")), pattern),
                      cb.like(cb.lower(root.get("idDocumentNumber")), pattern),
                      cb.like(cb.lower(root.get("nuib")), pattern)));
    }

    Page<Client> clients = clientRepository.findAll(spec, pageable);
    return ResponseEntity.ok(clients);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getClient(@PathVariable UUID id, Authentication authentication) {
    Organization organization =
        tenantAccessService.requireOrganization((User) authentication.getPrincipal());
    return clientRepository
        .findByIdAndOrganization(id, organization)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(404).body(ApiErrorBodies.notFound()));
  }

  public record UpdateRiskRatingRequest(@NotNull RiskRating riskRating) {}

  @PatchMapping("/{id}/risk-rating")
  @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
  public ResponseEntity<?> updateRiskRating(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateRiskRatingRequest request,
      Authentication authentication) {
    User actor = (User) authentication.getPrincipal();
    Client client =
        clientRepository
            .findByIdAndOrganization(id, tenantAccessService.requireOrganization(actor))
            .orElse(null);
    if (client == null) {
      return ResponseEntity.status(404).body(ApiErrorBodies.notFound());
    }

    client.setRiskRating(request.riskRating());
    client.setRiskRatingUpdatedAt(LocalDate.now());
    clientRepository.save(client);

    auditService.record(
        actor,
        AuditAction.CLIENT_UPDATED,
        "clients",
        client.getId(),
        "{\"field\": \"riskRating\", \"newValue\": \"" + request.riskRating() + "\"}");

    return ResponseEntity.ok(client);
  }

  @PatchMapping("/{id}/pep-review")
  @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
  public ResponseEntity<?> registerPepReview(@PathVariable UUID id, Authentication authentication) {
    User actor = (User) authentication.getPrincipal();
    Client client =
        clientRepository
            .findByIdAndOrganization(id, tenantAccessService.requireOrganization(actor))
            .orElse(null);
    if (client == null) {
      return ResponseEntity.status(404).body(ApiErrorBodies.notFound());
    }

    client.setPepLastReviewedAt(LocalDate.now());
    clientRepository.save(client);

    auditService.record(
        actor,
        AuditAction.PEP_REVIEWED,
        "clients",
        client.getId(),
        "{\"pepLastReviewedAt\": \"" + LocalDate.now() + "\"}");

    return ResponseEntity.ok(
        Map.of(
            "message",
            "Annual politically exposed person review recorded.",
            "date",
            LocalDate.now()));
  }

  public record CreateClientRequest(
      @NotBlank String fullName,
      @NotBlank String idDocumentType,
      @NotBlank String idDocumentNumber,
      ClientType clientType,
      String nuib,
      DueDiligenceRegime dueDiligenceRegime,
      boolean isPep,
      String pepCategory,
      boolean seniorManagementApproved) {}

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
  public ResponseEntity<?> createClient(
      @Valid @RequestBody CreateClientRequest request, Authentication authentication) {
    Client client =
        new Client(request.fullName(), request.idDocumentType(), request.idDocumentNumber());
    client.setOrganization(
        tenantAccessService.requireOrganization((User) authentication.getPrincipal()));
    if (request.clientType() != null) {
      client.setClientType(request.clientType());
    }
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
    auditService.record(
        actor, AuditAction.CLIENT_UPDATED, "clients", saved.getId(), "{\"action\": \"CREATED\"}");

    return ResponseEntity.ok(saved);
  }
}
